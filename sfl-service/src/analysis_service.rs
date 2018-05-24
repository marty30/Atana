use get_settings;
use models::analysis_result::AnalysisResult;
use models::coverage_matrix::CoverageMatrix;
use models::model::State;
use models::model::TestModel;
use models::model::Transition;
use models::test_case::TestCase;
use models::test_case::TestResult;
use send_progress;
use settings::Settings;
use std::collections::HashMap;
use std::collections::HashSet;
use std::mem;
use std::sync::Mutex;
use std::sync::MutexGuard;
use std::thread;
use storage_service::get_storage_service;
use models::test_case::Step;
use std::cmp::max;
use std::fs::File;

const SERVICE: AnalysisService = AnalysisService {};

lazy_static! {
    static ref TEMP_ANALYSIS_SERVICE: Mutex<Option<&'static (AnalysisServiceTrait + Sync)>> = Mutex::new(Some(&SERVICE));
    static ref SETTINGS: Settings = Settings::unwrap(Settings::new());
}

pub fn get_analysis_service<'a>() -> MutexGuard<'a, Option<&'static (AnalysisServiceTrait + Sync)>> {
    return match TEMP_ANALYSIS_SERVICE.lock() {
        Ok(service) => service,
        Err(poisoned) => poisoned.into_inner()
    };
}

pub fn set_analysis_service(new_service: &'static (AnalysisServiceTrait + Sync)) {
    let mut state = TEMP_ANALYSIS_SERVICE.lock().expect("Could not lock mutex");
    mem::replace(&mut *state, Some(new_service));
}

pub trait AnalysisServiceTrait {
    fn train(&self);
    fn analyse(&self, testcase: TestCase) -> Option<AnalysisResult>;
    fn similarity_coefficient(&self, step_j: &str, coverage_matrix: &CoverageMatrix) -> f32;
}

pub struct AnalysisService {}

impl AnalysisServiceTrait for AnalysisService {
    //noinspection RsTypeCheck
    fn train(&self) {
        send_progress(0.1);
        let matrix = {
            send_progress(0.2);
            let coverage_information: Vec<TestModel> = (&get_storage_service().coverage_information).to_owned();
            CoverageMatrix::from_with_progress(coverage_information, None, 0.25, 0.60)
        };

        let pair_matrix = {
            let storage_service = get_storage_service();
            let mut pairs = vec![];
            for len in 2..(get_settings().analysis.number_of_pairs_to_include_for_order+2) {
                storage_service.passing.iter().chain(storage_service.failing.iter()).for_each(|testcase|{
                    pairs.append(&mut get_pairs_with_length(len as usize, &(testcase.steps)));
                });
            }

            let transition_pairs = pairs.iter().map(|it| it.iter().filter_map(|it2|it2.to_transition(storage_service.model.as_ref().unwrap())).collect::<Vec<Transition>>()).collect::<Vec<Vec<_>>>();
            let coverage_information: Vec<TestModel> = (&storage_service.coverage_information).to_owned();
            CoverageMatrix::from_with_progress(coverage_information, Some(transition_pairs), 0.6, 0.9)
        };
        //add transition pairs to the coverage matrix
        let combined = matrix.append(pair_matrix);
        if combined.is_ok() {
            get_storage_service().set_coverage_matrix(combined.unwrap());
        }
        else {
            eprintln!("{}. Falling back to only the basic matrix.", combined.unwrap_err());
            get_storage_service().set_coverage_matrix(matrix);

        }
        send_progress(0.95);
        thread::spawn(|| (&get_storage_service().coverage_matrix.as_ref().unwrap()).to_coverage_table().to_csv(File::create("output.csv").unwrap()));
        send_progress(0.98);
    }

    //noinspection RsTypeCheck
    fn analyse(&self, testcase: TestCase) -> Option<AnalysisResult> {
        //Do not analyse test cases that have passed
        if testcase.verdict == Some(TestResult::passed) {
            return Some(AnalysisResult::new(String::from("Correct"), None, None, None));
        }

        //Extract the relevant problematic steps
        let mut problematic_steps: Vec<Transition> = vec![];
        let coverage_model = { testcase.to_coverage_model() };
        let storage_service = get_storage_service();
        let model = &storage_service.model;
        let coverage_model_transitions = coverage_model.unwrap_or((model.as_ref().unwrap()).clone()).all_transitions();
        let covered_transitions = coverage_model_transitions.iter().filter(|it|it.attributes.covered.unwrap_or(false) == true).collect::<Vec<_>>();

        for transition in covered_transitions {
            let step = transition.to_step(&testcase.steps);
            if step.is_some() && testcase.steps.contains(&step.unwrap()){
                let similarity_coefficient = self.similarity_coefficient(&transition.to_string(), &storage_service.coverage_matrix.as_ref().unwrap());
                println!("similarity_coefficient is {} for \tstep {:?}\t in {:?} ", similarity_coefficient, transition.to_string(), testcase.get_steps());
                if similarity_coefficient > SETTINGS.analysis.similarity_threshold {
                    problematic_steps.push(transition.to_owned());
                }
            }
        }

        let mut pairs = vec![];
        for len in 2..(get_settings().analysis.number_of_pairs_to_include_for_order+2) {
            pairs.append(&mut get_pairs_with_length(len as usize, &testcase.steps));
        }

        let transition_pairs = pairs.iter().map(|it| it.iter().filter_map(|it2|it2.to_transition(model.as_ref().unwrap())).collect::<Vec<Transition>>()).collect::<Vec<Vec<_>>>();
        for mut transition_pair in transition_pairs {
            let step_pair = transition_pair.iter().map(|transition| transition.to_step(&testcase.steps)).collect::<Vec<Option<Step>>>();
            if step_pair.iter().all(|step| step.is_some() && testcase.steps.contains(step.as_ref().unwrap())) {
                let string_pair = transition_pair.iter().map(|it| it.to_string()).collect::<Vec<_>>().join(" ");
                let similarity_coefficient = self.similarity_coefficient(&string_pair, &storage_service.coverage_matrix.as_ref().unwrap());
                println!("similarity_coefficient is {} for \tstep {:?}\t in {:?} ", similarity_coefficient, string_pair, testcase.get_steps());
                if similarity_coefficient > SETTINGS.analysis.similarity_threshold {
                    problematic_steps.append(&mut transition_pair);
                }
            }
        }

        problematic_steps.sort();
        problematic_steps.dedup();
        let mut step_tuples: Vec<(String, String)> = problematic_steps.iter().map(|x| (x.source.clone(), x.attributes.label.clone())).collect::<Vec<_>>();
        step_tuples.sort_by(|a, b| a.0.cmp(&b.0));
        let mut step_labels = step_tuples.iter().map(|ref x| &x.1).collect::<Vec<_>>();
        step_labels.dedup();

        if problematic_steps.len() == 0 {
            return Some(AnalysisResult::new(String::from("No problematic steps found"), None, None, None));
        } else if problematic_steps.len() == 1 {
            let faulty_transition_val = problematic_steps[0].to_owned();
            return Some(AnalysisResult::new(String::from("Transition ") + &faulty_transition_val.attributes.label, None, Some(faulty_transition_val), Some(problematic_steps.iter().filter_map(|it| it.to_step(&testcase.steps)).collect::<Vec<_>>())));
        } else if problematic_steps.len() == 2 {
            let faulty_state = extract_state_from_transitions(&problematic_steps[0], &problematic_steps[1], model.as_ref().unwrap());
            return match faulty_state {
                Ok(faulty_state_val) => Some(AnalysisResult::new(String::from("State ") + &faulty_state_val.id, Some(faulty_state_val), None, Some(problematic_steps.iter().filter_map(|it| it.to_step(&testcase.steps)).collect::<Vec<_>>()))),
                Err(err_msg) => {
                    println!("{}", err_msg);
                    return Some(AnalysisResult::new(format!("Multiple steps: {:?}", step_labels), None, None, Some(problematic_steps.iter().filter_map(|it| it.to_step(&testcase.steps)).collect::<Vec<_>>())));
                }
            };
        } else {
            //It could not be limited to a single transition or state, so we will return all steps
            return Some(AnalysisResult::new(format!("Multiple steps: {:?}", step_labels), None, None, Some(problematic_steps.iter().filter_map(|it| it.to_step(&testcase.steps)).collect::<Vec<_>>())));
        }
    }

    fn similarity_coefficient(&self, step_j: &str, coverage_matrix: &CoverageMatrix) -> f32 {
        //Ochiai similarity coefficient
        // a_pq(j) = |{ i | o_ij = p ∧ e_i = q }|, where p and q are either 0 or 1.
        // o_ij is either 0 or 1, depending on if component j is covered by test run i.
        match coverage_matrix.data.get(step_j) {
            Some(col) => {
                let mut a_11 = 0;
                let mut a_01 = 0;
                let mut a_10 = 0;
                for (o_ij, e) in col.iter().zip(coverage_matrix.error_vector.iter()) {
                    if o_ij == &true && e == &true {
                        a_11 += 1;
                    } else if o_ij == &false && e == &true {
                        a_01 += 1;
                    } else if o_ij == &true && e == &false {
                        a_10 += 1;
                    }
                }

                return a_11 as f32 / ((((a_11 + a_01) * (a_11 + a_10)) as f32).sqrt());
            }
            None => {
                println!("Could not find a column for {}. So returning 0 as the similarity_coefficient", step_j);
                return 0 as f32;
            }
        }
    }
}

fn get_pairs_with_length<T: Clone>(pair_len: usize, haystack: &Vec<T>) -> Vec<Vec<T>> {
    let mut all_pairs = vec![];
    let number_of_loops = if haystack.len() < pair_len { 0 } else { max(haystack.len() + 1 - pair_len, 0) };
    for i in 0..number_of_loops {
        let mut cur_pair = vec![];
        for p in 0..pair_len {
            cur_pair.push(haystack[i + p].clone());
        }
        if cur_pair.len() > 0 {all_pairs.push(cur_pair);}
    }
    all_pairs
}

fn extract_state_from_transitions(transition1: &Transition, transition2: &Transition, model: &TestModel) -> Result<State, String> {
    let mut sources = HashSet::new();
    let mut targets = HashSet::new();

    sources.insert(&transition1.source);
    sources.insert(&transition2.source);
    targets.insert(&transition1.target);
    targets.insert(&transition2.target);

    let state_ids = sources.intersection(&targets).map(|it| it.to_owned()).cloned().collect::<Vec<String>>();
    let state_id = &state_ids[0];
    let states = model.stss.iter().map(|it| it.states.to_owned()).flatten().filter(|it| it.id == *state_id).collect::<Vec<State>>();
    return Ok(states[0].to_owned());
}

#[cfg(test)]
pub mod tests {
    use super::*;
    use models::test_case::Label;
    use models::test_case::Step;
    use chrono::DateTime;
    use std::time::SystemTime;
    use models::test_case::TestResult;
    use models::test_case::TestRun;
    use models::model::Transition;
    use models::model::State;
    use models::model::StateAttribute;
    use models::model::TestModel;
    use uuid::Uuid;
    use models::model::Sts;
    use models::model::TransitionAttribute;
    use models::model::TraceProperties;
    use std::fs::File;
    use std::path::PathBuf;
    use serde_json::from_reader;
    use std::fs;

    pub struct MockAnalysisService {
        pub next_result: Option<AnalysisResult>
    }

    impl AnalysisServiceTrait for MockAnalysisService {
        fn train(&self) {
            //Do nothing for now
        }

        fn analyse(&self, _testcase: TestCase) -> Option<AnalysisResult> {
            self.next_result.clone()
        }
        fn similarity_coefficient(&self, _step_j: &str, _coverage_matrix: &CoverageMatrix) -> f32 {
            //Do nothing for now
            return 0 as f32;
        }
    }

    #[test]
    fn test_analyse_with_faulty_state() {
        //Four test cases with each five steps, where there is one step that is failing.
        //  Model the tests are based of.
        //
        //                 a? +-+ a!          c! +-+
        //               +--->+2+----+      +--->+5|
        //  +-+ init +-+ |    +-+    |  +-+ |    +-+
        //  |0+----->+1+-+           +->+4+-+
        //  +-+      +-+ | b? +-+ b! |  +-+ | d! +-+
        //               +--->+3+----+      +--->+6|
        //                    +-+                +-+
        //
        //  State 3 is wrongly implemented and should be returned. There is no faulty transition

        //Given
        let state_0 = State::new(String::from("0"), StateAttribute::new(String::from("0"), String::from("integer"), None));
        let state_1 = State::new(String::from("1"), StateAttribute::new(String::from("1"), String::from("integer"), None));
        let state_2 = State::new(String::from("2"), StateAttribute::new(String::from("2"), String::from("integer"), None));
        let state_3 = State::new(String::from("3"), StateAttribute::new(String::from("3"), String::from("integer"), None));
        let state_4 = State::new(String::from("4"), StateAttribute::new(String::from("4"), String::from("integer"), None));
        let state_5 = State::new(String::from("5"), StateAttribute::new(String::from("5"), String::from("integer"), None));
        let state_6 = State::new(String::from("6"), StateAttribute::new(String::from("6"), String::from("integer"), None));

        let transition_init = Transition::new(String::from("0"), String::from("1"), TransitionAttribute::new(String::from("!init"), None, None));
        let transition_a_in = Transition::new(String::from("1"), String::from("2"), TransitionAttribute::new(String::from("?a"), None, None));
        let transition_b_in = Transition::new(String::from("1"), String::from("3"), TransitionAttribute::new(String::from("?b"), None, None));
        let transition_a_out = Transition::new(String::from("2"), String::from("4"), TransitionAttribute::new(String::from("!a"), None, None));
        let transition_b_out = Transition::new(String::from("3"), String::from("4"), TransitionAttribute::new(String::from("!b"), None, None));
        let transition_c_out = Transition::new(String::from("4"), String::from("5"), TransitionAttribute::new(String::from("!c"), None, None));
        let transition_d_out = Transition::new(String::from("4"), String::from("6"), TransitionAttribute::new(String::from("!d"), None, None));

        let model = TestModel::new(Uuid::new_v4(), vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![transition_init.clone(), transition_a_in.clone(), transition_b_in.clone(), transition_a_out.clone(), transition_b_out.clone(), transition_c_out.clone(), transition_d_out.clone()], vec![], Some(state_0.clone().id), Some(String::from("")), None)], None);

        let step_init = Step::new(Label::new(String::from("init"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 0, None, None, None, None);
        let step_a_in = Step::new(Label::new(String::from("a"), String::from("in"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 1, None, None, None, None);
        let step_b_in = Step::new(Label::new(String::from("b"), String::from("in"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 1, None, None, None, None);
        let step_a_out = Step::new(Label::new(String::from("a"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 2, None, None, None, None);
        let step_b_out = Step::new(Label::new(String::from("b"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 2, None, None, None, None);
        let step_c_out = Step::new(Label::new(String::from("c"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 3, None, None, None, None);
        let step_d_out = Step::new(Label::new(String::from("d"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 3, None, None, None, None);

        let passing_test_one = TestCase::new(Some(1), Some(TestResult::passed), None, vec![step_init.clone(), step_a_in.clone(), step_a_out.clone(), step_c_out.clone()], 6, None, None);
        let passing_test_two = TestCase::new(Some(2), Some(TestResult::passed), None, vec![step_init.clone(), step_a_in, step_a_out, step_d_out.clone()], 6, None, None);
        let failing_test_one = TestCase::new(Some(3), Some(TestResult::failed), Some(String::from("There is a failure")), vec![step_init.clone(), step_b_in.clone(), step_b_out.clone(), step_c_out.clone()], 6, None, None);
        let failing_test_two = TestCase::new(Some(4), Some(TestResult::failed), Some(String::from("There is a failure")), vec![step_init.clone(), step_b_in.clone(), step_b_out.clone(), step_d_out], 6, None, None);

        let model_passing_test_one = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(false))
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(true))))], Some(1));
        let model_passing_test_two = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(true)),
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(true))))], Some(2));
        let model_failing_test_one = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(false)),
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(false))))], Some(3));
        let model_failing_test_two = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(true)),
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(false))))], Some(4));

        get_storage_service().set_model(Some(model));
        get_storage_service().set_passing_tests(vec![passing_test_one, passing_test_two]);
        get_storage_service().set_failing_tests(vec![failing_test_one, failing_test_two]);
        get_storage_service().set_coverage_informations(vec![model_passing_test_one, model_passing_test_two, model_failing_test_one, model_failing_test_two]);

        //When
        get_settings().analysis.number_of_pairs_to_include_for_order = 0;
        let test_testcase = TestCase::new(Some(3), Some(TestResult::failed), Some(String::from("There is a failure")), vec![step_init.clone(), step_b_in.clone(), step_b_out.clone(), step_c_out.clone()], 6, None, None);
        get_analysis_service().unwrap().train();
        let analysis_result = get_analysis_service().unwrap().analyse(test_testcase);

        //Then
        assert_eq!(analysis_result.unwrap(), AnalysisResult::new(String::from("State 3"), Some(State::new(String::from("3"), StateAttribute::new(String::from("3"), String::from("integer"), None))), None, Some(vec![step_b_in.clone(), step_b_out.clone()])));
    }

    #[test]
    fn test_analyse_with_faulty_transition() {
        //Four test cases with each five steps, where there is one step that is failing.
        //  Model the tests are based of.
        //
        //                 a? +-+ a!          c! +-+
        //               +--->+2+----+      +--->+5|
        //  +-+ init +-+ |    +-+    |  +-+ |    +-+
        //  |0+----->+1+-+           +->+4+-+
        //  +-+      +-+ | b? +-+ b! |  +-+ | d! +-+
        //               +--->+3+----+      +--->+6|
        //                    +-+                +-+
        //
        //  Transition b? is wrongly implemented (as it goes to state 2, instead of state 3) and should be returned. There is no faulty state

        //Given
        let state_0 = State::new(String::from("0"), StateAttribute::new(String::from("0"), String::from("integer"), None));
        let state_1 = State::new(String::from("1"), StateAttribute::new(String::from("1"), String::from("integer"), None));
        let state_2 = State::new(String::from("2"), StateAttribute::new(String::from("2"), String::from("integer"), None));
        let state_3 = State::new(String::from("3"), StateAttribute::new(String::from("3"), String::from("integer"), None));
        let state_4 = State::new(String::from("4"), StateAttribute::new(String::from("4"), String::from("integer"), None));
        let state_5 = State::new(String::from("5"), StateAttribute::new(String::from("5"), String::from("integer"), None));
        let state_6 = State::new(String::from("6"), StateAttribute::new(String::from("6"), String::from("integer"), None));

        let transition_init = Transition::new(String::from("0"), String::from("1"), TransitionAttribute::new(String::from("!init"), None, None));
        let transition_a_in = Transition::new(String::from("1"), String::from("2"), TransitionAttribute::new(String::from("?a"), None, None));
        let transition_b_in = Transition::new(String::from("1"), String::from("3"), TransitionAttribute::new(String::from("?b"), None, None));
        let transition_a_out = Transition::new(String::from("2"), String::from("4"), TransitionAttribute::new(String::from("!a"), None, None));
        let transition_b_out = Transition::new(String::from("3"), String::from("4"), TransitionAttribute::new(String::from("!b"), None, None));
        let transition_c_out = Transition::new(String::from("4"), String::from("5"), TransitionAttribute::new(String::from("!c"), None, None));
        let transition_d_out = Transition::new(String::from("4"), String::from("6"), TransitionAttribute::new(String::from("!d"), None, None));

        let step_init = Step::new(Label::new(String::from("init"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 0, None, None, None, None);
        let step_a_in = Step::new(Label::new(String::from("a"), String::from("in"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 1, None, None, None, None);
        let step_b_in = Step::new(Label::new(String::from("b"), String::from("in"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 2, None, None, None, None);
        let step_a_out = Step::new(Label::new(String::from("a"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 3, None, None, None, None);
        let _step_b_out = Step::new(Label::new(String::from("b"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 4, None, None, None, None);
        let step_c_out = Step::new(Label::new(String::from("c"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 5, None, None, None, None);
        let step_d_out = Step::new(Label::new(String::from("d"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 6, None, None, None, None);

        let passing_test_one = TestCase::new(Some(1), Some(TestResult::passed), None, vec![step_init.clone(), step_a_in.clone(), step_a_out.clone(), step_c_out.clone()], 6, None, None);
        let passing_test_two = TestCase::new(Some(2), Some(TestResult::passed), None, vec![step_init.clone(), step_a_in, step_a_out.clone(), step_d_out.clone()], 6, None, None);
        let failing_test_one = TestCase::new(Some(3), Some(TestResult::failed), Some(String::from("There is a failure")), vec![step_init.clone(), step_b_in.clone(), step_a_out.clone(), step_c_out.clone()], 6, None, None);
        let failing_test_two = TestCase::new(Some(4), Some(TestResult::failed), Some(String::from("There is a failure")), vec![step_init.clone(), step_b_in.clone(), step_a_out.clone(), step_d_out], 6, None, None);

        let model = TestModel::new(Uuid::new_v4(), vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![transition_init.clone(), transition_a_in.clone(), transition_b_in.clone(), transition_a_out.clone(), transition_b_out.clone(), transition_c_out.clone(), transition_d_out.clone()], vec![], Some(state_0.clone().id), Some(String::from("")), None)], None);
        let model_passing_test_one = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(false))
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(true))))], Some(1));
        let model_passing_test_two = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(true)),
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(true))))], Some(2));
        let model_failing_test_one = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(false)),
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(false))))], Some(3));
        let model_failing_test_two = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(true)),
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(false))))], Some(4));

        get_storage_service().set_model(Some(model));
        get_storage_service().set_passing_tests(vec![passing_test_one, passing_test_two]);
        get_storage_service().set_failing_tests(vec![failing_test_one, failing_test_two]);
        get_storage_service().set_coverage_informations(vec![model_passing_test_one, model_passing_test_two, model_failing_test_one, model_failing_test_two]);

        //When
        get_settings().analysis.number_of_pairs_to_include_for_order = 0;
        let test_testcase = TestCase::new(Some(3), Some(TestResult::failed), Some(String::from("There is a failure")), vec![step_init.clone(), step_b_in.clone(), step_a_out.clone(), step_c_out.clone()], 6, None, None);
        get_analysis_service().unwrap().train();
        let analysis_result = get_analysis_service().unwrap().analyse(test_testcase);

        //Then
        assert_eq!(analysis_result.unwrap(), AnalysisResult::new(String::from("Transition ?b"), None, Some(transition_b_in), Some(vec![step_b_in.clone()])))
    }

    #[test]
    fn test_analyse_with_only_passing_tests() {
        //Four test cases with each five steps, where there none that are failing.
        //  Model the tests are based of.
        //
        //                 a? +-+ a!          c! +-+
        //               +--->+2+----+      +--->+5|
        //  +-+ init +-+ |    +-+    |  +-+ |    +-+
        //  |0+----->+1+-+           +->+4+-+
        //  +-+      +-+ | b? +-+ b! |  +-+ | d! +-+
        //               +--->+3+----+      +--->+6|
        //                    +-+                +-+
        //

        //Given
        let step_init = Step::new(Label::new(String::from("init"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 0, None, None, None, None);
        let step_a_in = Step::new(Label::new(String::from("a"), String::from("in"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 1, None, None, None, None);
        let step_b_in = Step::new(Label::new(String::from("b"), String::from("in"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 2, None, None, None, None);
        let step_a_out = Step::new(Label::new(String::from("a"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 3, None, None, None, None);
        let step_b_out = Step::new(Label::new(String::from("b"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 4, None, None, None, None);
        let step_c_out = Step::new(Label::new(String::from("c"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 5, None, None, None, None);
        let step_d_out = Step::new(Label::new(String::from("d"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 6, None, None, None, None);

        let passing_test_one = TestCase::new(Some(1), Some(TestResult::passed), None, vec![step_init.clone(), step_a_in.clone(), step_a_out.clone(), step_c_out.clone()], 6, None, None);
        let passing_test_two = TestCase::new(Some(2), Some(TestResult::passed), None, vec![step_init.clone(), step_a_in, step_a_out, step_d_out.clone()], 6, None, None);
        let failing_test_one = TestCase::new(Some(3), Some(TestResult::passed), None, vec![step_init.clone(), step_b_in.clone(), step_b_out.clone(), step_c_out.clone()], 6, None, None);
        let failing_test_two = TestCase::new(Some(4), Some(TestResult::passed), None, vec![step_init.clone(), step_b_in.clone(), step_b_out.clone(), step_d_out], 6, None, None);

        let state_0 = State::new(String::from("0"), StateAttribute::new(String::from("0"), String::from("integer"), None));
        let state_1 = State::new(String::from("1"), StateAttribute::new(String::from("1"), String::from("integer"), None));
        let state_2 = State::new(String::from("2"), StateAttribute::new(String::from("2"), String::from("integer"), None));
        let state_3 = State::new(String::from("3"), StateAttribute::new(String::from("3"), String::from("integer"), None));
        let state_4 = State::new(String::from("4"), StateAttribute::new(String::from("4"), String::from("integer"), None));
        let state_5 = State::new(String::from("5"), StateAttribute::new(String::from("5"), String::from("integer"), None));
        let state_6 = State::new(String::from("6"), StateAttribute::new(String::from("6"), String::from("integer"), None));

        let transition_init = Transition::new(String::from("0"), String::from("1"), TransitionAttribute::new(String::from("!init"), None, None));
        let transition_a_in = Transition::new(String::from("1"), String::from("2"), TransitionAttribute::new(String::from("?a"), None, None));
        let transition_b_in = Transition::new(String::from("1"), String::from("3"), TransitionAttribute::new(String::from("?b"), None, None));
        let transition_a_out = Transition::new(String::from("2"), String::from("4"), TransitionAttribute::new(String::from("!a"), None, None));
        let transition_b_out = Transition::new(String::from("3"), String::from("4"), TransitionAttribute::new(String::from("!b"), None, None));
        let transition_c_out = Transition::new(String::from("4"), String::from("5"), TransitionAttribute::new(String::from("!c"), None, None));
        let transition_d_out = Transition::new(String::from("4"), String::from("6"), TransitionAttribute::new(String::from("!d"), None, None));

        let model = TestModel::new(Uuid::new_v4(), vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1, state_2, state_3, state_4, state_5, state_6], vec![], vec![state_0.clone().to_start_state()], vec![transition_init, transition_a_in, transition_b_in, transition_a_out, transition_b_out, transition_c_out, transition_d_out], vec![], Some(state_0.id), Some(String::from("")), None)], None);
        get_storage_service().set_model(Some(model));
        get_storage_service().set_passing_tests(vec![passing_test_one, passing_test_two, failing_test_one, failing_test_two]);
        get_storage_service().set_failing_tests(vec![]);
        get_storage_service().set_coverage_informations(vec![]);


        //When
        let test_testcase = TestCase::new(Some(3), Some(TestResult::failed), Some(String::from("There is a failure")), vec![step_init.clone(), step_b_in.clone(), step_b_out.clone(), step_c_out.clone()], 6, None, None);
        get_analysis_service().unwrap().train();
        let analysis_result = get_analysis_service().unwrap().analyse(test_testcase);

        //Then
        assert_eq!(analysis_result.unwrap(), AnalysisResult::new(String::from("No problematic steps found"), None, None, None));
    }

    #[test]
    fn test_analyse_with_failure_based_on_order() {
        //Four test cases with each five steps, where there is one step that is failing if another transition is also taken.
        //  Model the tests are based of.
        //
        //                 a? +-+ a!          c! +-+
        //               +--->+2+----+      +--->+5|
        //  +-+ init +-+ |    +-+    |  +-+ |    +-+
        //  |0+----->+1+-+           +->+4+-+
        //  +-+      +-+ | b? +-+ b! |  +-+ | d! +-+
        //               +--->+3+----+      +--->+6|
        //                    +-+                +-+
        //
        //  Transition d! is wrongly implemented if the transition a! is taken and should be returned.

        //Given
        let state_0 = State::new(String::from("0"), StateAttribute::new(String::from("0"), String::from("integer"), None));
        let state_1 = State::new(String::from("1"), StateAttribute::new(String::from("1"), String::from("integer"), None));
        let state_2 = State::new(String::from("2"), StateAttribute::new(String::from("2"), String::from("integer"), None));
        let state_3 = State::new(String::from("3"), StateAttribute::new(String::from("3"), String::from("integer"), None));
        let state_4 = State::new(String::from("4"), StateAttribute::new(String::from("4"), String::from("integer"), None));
        let state_5 = State::new(String::from("5"), StateAttribute::new(String::from("5"), String::from("integer"), None));
        let state_6 = State::new(String::from("6"), StateAttribute::new(String::from("6"), String::from("integer"), None));

        let transition_init = Transition::new(String::from("0"), String::from("1"), TransitionAttribute::new(String::from("!init"), None, None));
        let transition_a_in = Transition::new(String::from("1"), String::from("2"), TransitionAttribute::new(String::from("?a"), None, None));
        let transition_b_in = Transition::new(String::from("1"), String::from("3"), TransitionAttribute::new(String::from("?b"), None, None));
        let transition_a_out = Transition::new(String::from("2"), String::from("4"), TransitionAttribute::new(String::from("!a"), None, None));
        let transition_b_out = Transition::new(String::from("3"), String::from("4"), TransitionAttribute::new(String::from("!b"), None, None));
        let transition_c_out = Transition::new(String::from("4"), String::from("5"), TransitionAttribute::new(String::from("!c"), None, None));
        let transition_d_out = Transition::new(String::from("4"), String::from("6"), TransitionAttribute::new(String::from("!d"), None, None));

        let model = TestModel::new(Uuid::new_v4(), vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![transition_init.clone(), transition_a_in.clone(), transition_b_in.clone(), transition_a_out.clone(), transition_b_out.clone(), transition_c_out.clone(), transition_d_out.clone()], vec![], Some(state_0.clone().id), Some(String::from("")), None)], None);

        let step_init = Step::new(Label::new(String::from("init"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 0, None, None, None, None);
        let step_a_in = Step::new(Label::new(String::from("a"), String::from("in"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 1, None, None, None, None);
        let step_b_in = Step::new(Label::new(String::from("b"), String::from("in"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 1, None, None, None, None);
        let step_a_out = Step::new(Label::new(String::from("a"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 2, None, None, None, None);
        let step_b_out = Step::new(Label::new(String::from("b"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 2, None, None, None, None);
        let step_c_out = Step::new(Label::new(String::from("c"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 3, None, None, None, None);
        let step_d_out = Step::new(Label::new(String::from("d"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 3, None, None, None, None);

        let passing_test_one = TestCase::new(Some(1), Some(TestResult::passed), None, vec![step_init.clone(), step_a_in.clone(), step_a_out.clone(), step_c_out.clone()], 6, None, None);
        let passing_test_two = TestCase::new(Some(2), Some(TestResult::passed), None, vec![step_init.clone(), step_b_in.clone(), step_b_out.clone(), step_c_out.clone()], 6, None, None);
        let passing_test_three = TestCase::new(Some(3), Some(TestResult::passed), None, vec![step_init.clone(), step_b_in.clone(), step_b_out.clone(), step_d_out.clone()], 6, None, None);
        let failing_test_one = TestCase::new(Some(4), Some(TestResult::failed), Some(String::from("There is a failure")), vec![step_init.clone(), step_a_in.clone(), step_a_out.clone(), step_d_out.clone()], 6, None, None);

        let model_passing_test_one = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(false))
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(true))))], Some(1));
        let model_passing_test_two = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(false)),
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(true))))], Some(2));
        let model_passing_test_three = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(true)),
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(true))))], Some(3));
        let model_failing_test_one = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(true)),
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(false))))], Some(4));

        get_storage_service().set_model(Some(model));
        get_storage_service().set_passing_tests(vec![passing_test_one, passing_test_two, passing_test_three]);
        get_storage_service().set_failing_tests(vec![failing_test_one]);
        get_storage_service().set_coverage_informations(vec![model_passing_test_one, model_passing_test_two, model_passing_test_three, model_failing_test_one]);

        //When
        get_settings().analysis.number_of_pairs_to_include_for_order = 2;
        let test_testcase = TestCase::new(Some(4), Some(TestResult::failed), Some(String::from("There is a failure")), vec![step_init.clone(), step_a_in.clone(), step_a_out.clone(), step_d_out.clone()], 6, None, None);
        get_analysis_service().unwrap().train();
        let analysis_result = get_analysis_service().unwrap().analyse(test_testcase);

        //Then
        //Require that both a? and d! are in the results and preferably at most one more
        let unwrapped_result = analysis_result.unwrap();
        eprintln!("unwrapped_result = {:?}", unwrapped_result);
        assert!(&unwrapped_result.root_cause_steps.len() <= &3, format!("The analysis result contains more than 3 steps, i.e. {:?}. The result: {:?}", unwrapped_result.root_cause_steps.len(), unwrapped_result));
        assert!(&unwrapped_result.root_cause_steps.contains(&step_a_out), format!("The analysis result does not contain the step !a. The result: {:?}", unwrapped_result));
        assert!(&unwrapped_result.root_cause_steps.contains(&step_d_out), format!("The analysis result does not contain the step !d. The result: {:?}", unwrapped_result));
//        assert_eq!(analysis_result.unwrap(), AnalysisResult::new(String::from("Transition !d"), None, Some(transition_d_out.clone()), Some(vec![step_d_out.clone()])));
    }

    #[test]
    fn test_analyse_with_failure_based_on_order_with_tau() {
        //Four test cases with each five steps, where there is one step that is failing if another transition is also taken.
        //  Model the tests are based of.
        //
        //                 a? +-+ a!                c! +-+
        //               +--->+2+----+            +--->+5|
        //  +-+ init +-+ |    +-+    |  +-+ T +-+ |    +-+
        //  |0+----->+1+-+           +->+7+-->+4+-+
        //  +-+      +-+ | b? +-+ b! |  +-+   +-+ | d! +-+
        //               +--->+3+----+            +--->+6|
        //                    +-+                      +-+
        //
        //  Transition d! is wrongly implemented if the transition a! is taken and should be returned. There is a transition between state 7 and 4 that is a tau step, so no label is added to the transition.

        //Given
        let state_0 = State::new(String::from("0"), StateAttribute::new(String::from("0"), String::from("integer"), None));
        let state_1 = State::new(String::from("1"), StateAttribute::new(String::from("1"), String::from("integer"), None));
        let state_2 = State::new(String::from("2"), StateAttribute::new(String::from("2"), String::from("integer"), None));
        let state_3 = State::new(String::from("3"), StateAttribute::new(String::from("3"), String::from("integer"), None));
        let state_4 = State::new(String::from("4"), StateAttribute::new(String::from("4"), String::from("integer"), None));
        let state_5 = State::new(String::from("5"), StateAttribute::new(String::from("5"), String::from("integer"), None));
        let state_6 = State::new(String::from("6"), StateAttribute::new(String::from("6"), String::from("integer"), None));
        let state_7 = State::new(String::from("7"), StateAttribute::new(String::from("7"), String::from("integer"), None));

        let transition_init = Transition::new(String::from("0"), String::from("1"), TransitionAttribute::new(String::from("!init"), None, None));
        let transition_a_in = Transition::new(String::from("1"), String::from("2"), TransitionAttribute::new(String::from("?a"), None, None));
        let transition_b_in = Transition::new(String::from("1"), String::from("3"), TransitionAttribute::new(String::from("?b"), None, None));
        let transition_a_out = Transition::new(String::from("2"), String::from("7"), TransitionAttribute::new(String::from("!a"), None, None));
        let transition_b_out = Transition::new(String::from("3"), String::from("7"), TransitionAttribute::new(String::from("!b"), None, None));
        let transition_c_out = Transition::new(String::from("4"), String::from("5"), TransitionAttribute::new(String::from("!c"), None, None));
        let transition_d_out = Transition::new(String::from("4"), String::from("6"), TransitionAttribute::new(String::from("!d"), None, None));
        let transition_tau = Transition::new(String::from("7"), String::from("4"), TransitionAttribute::new(String::from(""), Some(String::from("unobservable")), None));

        let model = TestModel::new(Uuid::new_v4(), vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone(), state_7.clone()], vec![], vec![state_0.clone().to_start_state()], vec![transition_init.clone(), transition_a_in.clone(), transition_b_in.clone(), transition_a_out.clone(), transition_b_out.clone(), transition_c_out.clone(), transition_d_out.clone(), transition_tau.clone()], vec![], Some(state_0.clone().id), Some(String::from("")), None)], None);

        let step_init = Step::new(Label::new(String::from("init"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 0, None, None, None, None);
        let step_a_in = Step::new(Label::new(String::from("a"), String::from("in"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 1, None, None, None, None);
        let step_b_in = Step::new(Label::new(String::from("b"), String::from("in"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 1, None, None, None, None);
        let step_a_out = Step::new(Label::new(String::from("a"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 2, None, None, None, None);
        let step_b_out = Step::new(Label::new(String::from("b"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 2, None, None, None, None);
        let step_c_out = Step::new(Label::new(String::from("c"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 3, None, None, None, None);
        let step_d_out = Step::new(Label::new(String::from("d"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 3, None, None, None, None);

        let passing_test_one = TestCase::new(Some(1), Some(TestResult::passed), None, vec![step_init.clone(), step_a_in.clone(), step_a_out.clone(), step_c_out.clone()], 6, None, None);
        let passing_test_two = TestCase::new(Some(2), Some(TestResult::passed), None, vec![step_init.clone(), step_b_in.clone(), step_b_out.clone(), step_c_out.clone()], 6, None, None);
        let passing_test_three = TestCase::new(Some(3), Some(TestResult::passed), None, vec![step_init.clone(), step_b_in.clone(), step_b_out.clone(), step_d_out.clone()], 6, None, None);
        let failing_test_one = TestCase::new(Some(4), Some(TestResult::failed), Some(String::from("There is a failure")), vec![step_init.clone(), step_a_in.clone(), step_a_out.clone(), step_d_out.clone()], 6, None, None);

        let model_passing_test_one = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_tau.clone_and_update(|it| it.attributes.covered = Some(true)),
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(true))))], Some(1));
        let model_passing_test_two = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_tau.clone_and_update(|it| it.attributes.covered = Some(true)),
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(true))))], Some(2));
        let model_passing_test_three = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_tau.clone_and_update(|it| it.attributes.covered = Some(true)),
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(true))))], Some(3));
        let model_failing_test_one = TestModel::new(model.test_run_id, vec![Sts::new(String::from("test model"), vec![state_0.clone(), state_1.clone(), state_2.clone(), state_3.clone(), state_4.clone(), state_5.clone(), state_6.clone()], vec![], vec![state_0.clone().to_start_state()], vec![
            transition_init.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_a_in.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_in.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_a_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_b_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_c_out.clone_and_update(|it| it.attributes.covered = Some(false)),
            transition_d_out.clone_and_update(|it| it.attributes.covered = Some(true)),
            transition_tau.clone_and_update(|it| it.attributes.covered = Some(true)),
        ], vec![], Some(state_0.clone().id), Some(String::from("")), Some(TraceProperties::new(Some(false))))], Some(4));

        get_storage_service().set_model(Some(model));
        get_storage_service().set_passing_tests(vec![passing_test_one, passing_test_two, passing_test_three]);
        get_storage_service().set_failing_tests(vec![failing_test_one]);
        get_storage_service().set_coverage_informations(vec![model_passing_test_one, model_passing_test_two, model_passing_test_three, model_failing_test_one]);

        //When
        get_settings().analysis.number_of_pairs_to_include_for_order = 2;
        let test_testcase = TestCase::new(Some(4), Some(TestResult::failed), Some(String::from("There is a failure")), vec![step_init.clone(), step_a_in.clone(), step_a_out.clone(), step_d_out.clone()], 6, None, None);
        get_analysis_service().unwrap().train();
        let analysis_result = get_analysis_service().unwrap().analyse(test_testcase);

        //Then
        //Require that both a? and d! are in the results and preferably at most one more
        let unwrapped_result = analysis_result.unwrap();
        eprintln!("unwrapped_result = {:?}", unwrapped_result);
        assert!(&unwrapped_result.root_cause_steps.len() <= &3, format!("The analysis result contains more than 3 steps, i.e. {:?}. The result: {:?}", unwrapped_result.root_cause_steps.len(), unwrapped_result));
        assert!(&unwrapped_result.root_cause_steps.contains(&step_a_out), format!("The analysis result does not contain the step !a. The result: {:?}", unwrapped_result));
        assert!(&unwrapped_result.root_cause_steps.contains(&step_d_out), format!("The analysis result does not contain the step !d. The result: {:?}", unwrapped_result));
    }


    use glob::glob;
    use std::result::Result;

    #[test]
//    fn real_testcase_example() {
//        println!("Starting test...");
//
//        let mut resources_folder = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
//        resources_folder.push("resources");
//        resources_folder.push("cre_server3_16");
//
//        println!("Define the right paths to the different files that are required.");
//
//        let mut tests_path = glob(&format!("{}\\{}",resources_folder.clone().to_str().unwrap_or("."), "SCRP*.json")).unwrap().filter_map(|it|  it.ok() ).next().unwrap();
//        let mut model_path = glob(&format!("{}\\{}",resources_folder.clone().to_str().unwrap_or("."), "model.json")).unwrap().filter_map(|it| it.ok() ).next().unwrap();
//        let mut test_models_paths: Vec<PathBuf> = glob(&format!("{}\\{}",resources_folder.clone().to_str().unwrap_or("."), "coverage_trace_model*.json")).unwrap().filter_map(|it| it.ok() ).collect::<Vec<PathBuf>>();
//
//        let model_file = File::open(model_path).expect("file not found");
//        let testcase_file = File::open(tests_path).expect("file not found");
//
//        println!("Loading everything from disk into memory...");
//
//        let model: TestModel = from_reader(model_file).unwrap();
//        let testrun: TestRun = from_reader(testcase_file).unwrap();
//        let testcases = testrun.test_cases;
//        let test_models = test_models_paths.iter().map(|it|from_reader(File::open(it).expect(&format!("file {} not found", it.display()))).unwrap()).collect::<Vec<TestModel>>();
//
//        println!("Everything is loaded from disk, storing them now...");
//
//        get_storage_service().set_model(Some(model.clone()));
//        get_storage_service().set_passing_tests(testcases.clone().iter().filter(|it|it.verdict == Some(TestResult::passed)).map(|it| it.clone()).collect::<Vec<TestCase>>());
//        get_storage_service().set_failing_tests(testcases.clone().iter().filter(|it|it.verdict == Some(TestResult::failed)).map(|it| it.clone()).collect::<Vec<TestCase>>());
//        get_storage_service().set_coverage_informations(test_models);
//
//        //When
//        get_settings().analysis.number_of_pairs_to_include_for_order = 2;
//        get_analysis_service().unwrap().train();
//        let analysis_result = get_analysis_service().unwrap().analyse(testcases[15].clone());
//        eprintln!("analysis_result = {:?}", analysis_result);
//    }

    /**
     * This test is based on an example given in "Refining spectrum-based fault localization rankings"
     * by Abreu, Rui and Mayer, Wolfgang and Stumptner, Markus and van Gemund, Arjan JC
     */
    #[test]
    fn check_similarity() {
        let translation = vec!["c1", "c2", "c3", "c4", "c6", "c7", "c9", "c12"].iter().map(|it| it.to_string()).collect::<Vec<String>>();
        let error = vec![true, false, false, false, false, true];
        let mut data = HashMap::new();
        data.insert("c1".to_string(), vec![true, true, true, true, true, true]);
        data.insert("c2".to_string(), vec![true, true, true, true, true, true, ]);
        data.insert("c3".to_string(), vec![true, false, true, true, true, true]);
        data.insert("c4".to_string(), vec![false, false, true, true, false, false]);
        data.insert("c6".to_string(), vec![true, false, false, true, true, true]);
        data.insert("c7".to_string(), vec![true, false, false, true, true, true]);
        data.insert("c9".to_string(), vec![true, false, false, true, true, true]);
        data.insert("c12".to_string(), vec![true, true, false, false, true, false]);

        let coverage_matrix = CoverageMatrix::new(error, vec![1, 2, 3, 4, 5, 6], data);
        coverage_matrix.to_coverage_table().printstd();

        let expected = vec![0.58, 0.58, 0.63, 0.00, 0.71, 0.71, 0.71, 0.41];
        let mut actual = vec![];
        for component in translation.iter() {
            actual.push((get_analysis_service().unwrap().similarity_coefficient(component, &coverage_matrix) * 100 as f32).round() / 100 as f32);
        }

        assert_eq!(expected, actual);
    }

    #[test]
    fn test_pair_generation() {
        let haystack = vec![1, 2, 3, 4, 5, 6, 7, 8, 9];

        let single_pairs = vec![vec![1], vec![2], vec![3], vec![4], vec![5], vec![6], vec![7], vec![8], vec![9]];
        let two_pairs = vec![vec![1, 2], vec![2, 3], vec![3, 4], vec![4, 5], vec![5, 6], vec![6, 7], vec![7, 8], vec![8, 9]];
        let three_pairs = vec![vec![1, 2, 3], vec![2, 3, 4], vec![3, 4, 5], vec![4, 5, 6], vec![5, 6, 7], vec![6, 7, 8], vec![7, 8, 9]];
        let four_pairs = vec![vec![1, 2, 3, 4], vec![2, 3, 4, 5], vec![3, 4, 5, 6], vec![4, 5, 6, 7], vec![5, 6, 7, 8], vec![6, 7, 8, 9]];
        let five_pairs = vec![vec![1, 2, 3, 4, 5], vec![2, 3, 4, 5, 6], vec![3, 4, 5, 6, 7], vec![4, 5, 6, 7, 8], vec![5, 6, 7, 8, 9]];
        let six_pairs = vec![vec![1, 2, 3, 4, 5, 6], vec![2, 3, 4, 5, 6, 7], vec![3, 4, 5, 6, 7, 8], vec![4, 5, 6, 7, 8, 9]];
        let seven_pairs = vec![vec![1, 2, 3, 4, 5, 6, 7], vec![2, 3, 4, 5, 6, 7, 8], vec![3, 4, 5, 6, 7, 8, 9]];
        let eight_pairs = vec![vec![1, 2, 3, 4, 5, 6, 7, 8], vec![2, 3, 4, 5, 6, 7, 8, 9]];
        let nine_pairs = vec![vec![1, 2, 3, 4, 5, 6, 7, 8, 9]];
        let empty : Vec<Vec<i32>>= vec![];

        assert_eq!(Vec::<Vec<i32>>::new(), get_pairs_with_length(0, &haystack));
        assert_eq!(single_pairs, get_pairs_with_length(1, &haystack));
        assert_eq!(two_pairs, get_pairs_with_length(2, &haystack));
        assert_eq!(three_pairs, get_pairs_with_length(3, &haystack));
        assert_eq!(four_pairs, get_pairs_with_length(4, &haystack));
        assert_eq!(five_pairs, get_pairs_with_length(5, &haystack));
        assert_eq!(six_pairs, get_pairs_with_length(6, &haystack));
        assert_eq!(seven_pairs, get_pairs_with_length(7, &haystack));
        assert_eq!(eight_pairs, get_pairs_with_length(8, &haystack));
        assert_eq!(nine_pairs, get_pairs_with_length(9, &haystack));
        assert_eq!(empty, get_pairs_with_length(10, &haystack));
        assert_eq!(empty, get_pairs_with_length(11, &haystack));
    }
}