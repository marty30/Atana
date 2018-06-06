use chrono::DateTime;
use chrono::Utc;
use models::model::TestModel;
use models::model::Transition;
use serde_json::Map;
use std::hash::Hash;
use std::hash::Hasher;
use std::path::PathBuf;
use std::fs::File;
use storage_service::get_storage_service;
use std::collections::HashMap;
use uuid::Uuid;
use serde_json::value::Value;
use std::cmp::Ordering;
use get_settings;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct TestRun {
    pub test_run_id: Option<Uuid>,
    pub test_cases: Vec<TestCase>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct TestCase {
    pub id: Option<i64>,
    pub verdict: Option<TestResult>,
    pub error_message: Option<String>,
    pub steps: Vec<Step>,
    pub last_step: i32,
    expected_labels: Option<Vec<ExpectedLabel>>,
    tags: Option<Vec<String>>,
}

impl TestCase {
    pub fn new(id: Option<i64>, verdict: Option<TestResult>, error_message: Option<String>, steps: Vec<Step>, last_step: i32, expected_labels: Option<Vec<ExpectedLabel>>, tags: Option<Vec<String>>) -> TestCase {
        TestCase {
            id,
            verdict,
            error_message,
            steps,
            last_step,
            expected_labels,
            tags,
        }
    }

    pub fn get_steps(&self) -> Vec<String> {
        self.steps.iter().map(|it| it.get_full_label()).collect()
    }

    pub fn get_step_tuples(&self) -> Vec<(Step, Step)> {
        self.get_step_combinations(2).iter().map(|it| (it[0].to_owned(), it[1].to_owned())).collect::<Vec<(Step, Step)>>()
    }

    pub fn get_step_combinations(&self, len: usize) -> Vec<Vec<Step>> {
        let mut step_combos = vec![];
        let mut prev = vec![];
        for step in &self.steps {
            if prev.len() == (len - 1) {
                prev.push(step.to_owned());
                step_combos.push(prev.clone());
                prev.remove(0); //Remove the first element
            } else if prev.len() < len {
                prev.push(step.to_owned());
            }
        }
        if step_combos.len() == 0 {
            step_combos.push(vec![]);
        }
        step_combos
    }

    //noinspection RsTypeCheck
    pub fn to_coverage_model(&self) -> Option<TestModel> {
        get_storage_service().coverage_information.iter().find(|it| it.testcase_id == self.id).map(|it| it.to_owned())
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Step {
    pub label: Label,
    timestamp: DateTime<Utc>,
    notes: Option<Vec<String>>,
    pub step_number: i32,
    state_vector_size: Option<String>,
    advance_duration_ms: Option<String>,
    physical_label: Option<String>,
    pub label_parameters: Option<Map<String, Value>>,
}

impl Step {
    pub fn new(label: Label, timestamp: DateTime<Utc>, notes: Option<Vec<String>>, step_number: i32, state_vector_size: Option<String>, advance_duration_ms: Option<String>, physical_label: Option<String>, label_parameters: Option<Map<String, Value>>) -> Step {
        Step {
            label,
            timestamp,
            notes,
            step_number,
            state_vector_size,
            advance_duration_ms,
            physical_label,
            label_parameters,
        }
    }

    pub fn to_transition(&self, model: &TestModel) -> Option<Transition> {
        for sts in &model.stss {
            for transition in &sts.transitions {
                if transition.attributes.label == self.get_full_label_with_params(true) {
                    return Some(transition.clone());
                }
            }
        }
        let mut alternatives = model.stss.iter().map(|it|&it.transitions).flatten().filter(|it|it.attributes.covered.unwrap_or(false) && it.attributes.label.starts_with(&self.get_full_label())).collect::<Vec<&Transition>>();
        alternatives.dedup();
        let alternatives_counted: HashMap<Transition, usize> = alternatives.iter().map(|it|{
            let label = &it.attributes.label.clone();
            let mut all_variables: Vec<String> = vec![];
                if let Some(ref label_parameters) = self.label_parameters {
                    let mut keys = label_parameters.keys().map(|it2|it2.clone()).collect::<Vec<String>>();
                    let mut values = label_parameters.values().filter_map(|it2|
                        match it2 {
                        Value::String(s) => Some(s.clone()),
                        Value::Bool(b) => Some(b.to_string()),
                        Value::Number(n) => Some(n.to_string()),
                        _ => None,
                    }).collect::<Vec<String>>();
                    all_variables.append(&mut keys);
                    all_variables.append(&mut values);
            }
            (it.clone().clone(), all_variables.iter().filter(|it2| label.contains(*it2)).count())
        }).collect();
        let result = alternatives_counted.iter().max_by(|first, second| first.1.cmp(&second.1));
        if result.is_some() {
            println!("No exactly matching transition found for {:?}. Fallback to {:?}.", self.get_full_label_with_params(true), result);//model.stss.iter().map(|it| it.transitions.clone()).flatten().map(|it| it.attributes.label).collect::<Vec<_>>());
        }
        else {
            println!("No exactly matching transition found for {:?}. No fallback found either.", self.get_full_label_with_params(true));
        }
        result.map(|it| it.0.clone())
    }

    pub fn get_full_label_with_params(&self, include_parameters: bool) -> String {
        let mut full_label = String::new();
        if &self.label.direction == "in" || &self.label.direction == "stimulus" {
            full_label.push('?')
        } else if self.label.direction == "out" || self.label.direction == "response" {
            full_label.push('!')
        }
        full_label.push_str(self.label.name.as_ref());

        if include_parameters {
            if let Some(ref label_params) = self.label_parameters {
                if !label_params.is_empty() {
                    full_label.push_str(" if (");
                    let mut label_param_args = vec![];
                    for label_param in label_params.iter() {
                        label_param_args.push("(".to_string() + label_param.0 + " == \"" + label_param.1.as_str().unwrap_or("NULL") + "\")")
                    }
                    full_label.push_str(&label_param_args.join(" && "));
                    full_label.push_str(")");
                }
            }
        }
        return full_label;
    }
    pub fn get_full_label(&self) -> String {
        self.get_full_label_with_params(get_settings().analysis.use_transition_data)
    }
}

impl Eq for Step {}

impl Hash for Step {
    fn hash<H: Hasher>(&self, state: &mut H) {
        self.label.hash(state);
        self.timestamp.hash(state);
        self.notes.hash(state);
        self.step_number.hash(state);
        self.state_vector_size.hash(state);
        self.advance_duration_ms.hash(state);
        self.physical_label.hash(state);
        //TODO optionally include the label_parameters as well
    }
}

impl Ord for Step{
    fn cmp(&self, other: &Self) -> Ordering {
        (&self.label, &self.step_number, &self.timestamp, &self.notes, &self.state_vector_size, &self.advance_duration_ms, &self.physical_label).cmp(&(&other.label, &other.step_number, &other.timestamp, &other.notes, &other.state_vector_size, &other.advance_duration_ms, &other.physical_label))
    }
}

impl PartialOrd for Step{
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(&other))
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq, Hash, Ord, PartialOrd)]
pub struct Label {
    pub name: String,
    direction: String,
    channel: Option<String>,
}

impl Label {
    pub fn new(name: String, direction: String, channel: Option<String>) -> Label {
        Label {
            name,
            direction,
            channel,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq, Hash)]
pub struct ExpectedLabel {
    label: Label,
//    deadline: Option<DateTime<Utc>>,
}

impl ExpectedLabel {
    pub fn new(label: Label, deadline: Option<DateTime<Utc>>) -> ExpectedLabel {
        ExpectedLabel {
            label,
//            deadline,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[allow(non_camel_case_types)]
pub enum TestResult {
    passed,
    failed,
    error,
    unknown,
}

#[cfg(test)]
pub mod tests {
    use super::*;
    use std::time::SystemTime;
    use serde_json::value::Value;
    use serde_json::from_reader;

    #[test]
    fn teststep_to_transition_label() {
        //Given
        let simple_step = Step::new(
            Label::new(String::from("C251_SIGNED_ON"), String::from("response"), Some(String::from("pos"))),
            DateTime::from(SystemTime::now()),
            Some(vec![]),
            8,
            Some("1".to_string()),
            Some("1".to_string()),
            Some(String::from("MjUxIFNpZ25lZCBPbg==\n")),
            Some(Map::new()),
        );
        //When
        let simple_step_full_label = simple_step.get_full_label();
        //Then
        assert_eq!(simple_step_full_label, "!C251_SIGNED_ON");

        //Given
        let mut label_parameters = Map::new();
        label_parameters.insert(String::from("return_variable_name"), Value::String(String::from("CS_SIGN")));
        label_parameters.insert(String::from("variable_value"), Value::String(String::from("SS_ON")));

        let parameterized_step = Step::new(
            Label::new(String::from("C210_VAR_RETURN"), String::from("response"), Some(String::from("pos"))),
            DateTime::from(SystemTime::now()),
            Some(vec![]),
            2,
            Some("1".to_string()),
            Some("1".to_string()),
            Some(String::from("MjEwIENTX1NJR046U1NfT04=\n")),
            Some(label_parameters),
        );
        //When
        let parameterized_step_full_label = parameterized_step.get_full_label_with_params(true);
        //Then
        assert_eq!(parameterized_step_full_label, "!C210_VAR_RETURN if ((return_variable_name == \"CS_SIGN\") && (variable_value == \"SS_ON\"))");
    }

    #[test]
    fn teststep_to_transition() {
        let mut resources_folder = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
        resources_folder.push("resources");

        let mut model_path = resources_folder.clone();
        model_path.push("model.json");
        let mut testcase_path = resources_folder.clone();
        testcase_path.push("testrun.json");

        let model_file = File::open(model_path).expect("file not found");
        let testcase_file = File::open(testcase_path).expect("file not found");

        let model: TestModel = from_reader(model_file).unwrap();
        let testcase: TestCase = from_reader(testcase_file).unwrap();

        let original_steps = testcase.steps;
        let transitions = original_steps.iter().map(|step| step.to_transition(&model)).collect::<Vec<_>>();
        let new_steps = transitions.iter().filter_map(|transition| transition.as_ref().unwrap().to_step(&original_steps)).collect::<Vec<_>>();
        assert_eq!(0, transitions.iter().filter(|it| it.is_none()).count());
        assert_eq!(original_steps.iter().map(|it| it.get_full_label()).collect::<Vec<_>>(), new_steps.iter().map(|it| it.get_full_label()).collect::<Vec<_>>());
    }

    #[test]
    fn check_extracting_step_combos() {
        let step_init = Step::new(Label::new(String::from("init"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 0, None, None, None, None);
        let step_a_in = Step::new(Label::new(String::from("a"), String::from("in"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 1, None, None, None, None);
//        let step_b_in = Step::new(Label::new(String::from("b"), String::from("in"), String::from("default")), DateTime::from(SystemTime::now()), None, 1, None, None, None, None);
        let step_a_out = Step::new(Label::new(String::from("a"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 2, None, None, None, None);
//        let step_b_out = Step::new(Label::new(String::from("b"), String::from("out"), String::from("default")), DateTime::from(SystemTime::now()), None, 2, None, None, None, None);
        let step_c_out = Step::new(Label::new(String::from("c"), String::from("out"), Some(String::from("default"))), DateTime::from(SystemTime::now()), None, 3, None, None, None, None);
//        let step_d_out = Step::new(Label::new(String::from("d"), String::from("out"), String::from("default")), DateTime::from(SystemTime::now()), None, 3, None, None, None, None);

        let testcase = TestCase::new(Some(1), Some(TestResult::passed), None, vec![step_init.clone(), step_a_in.clone(), step_a_out.clone(), step_c_out.clone()], 6, None, None);
        assert_eq!(vec![vec![step_init.get_full_label()], vec![step_a_in.get_full_label()], vec![step_a_out.get_full_label()], vec![step_c_out.get_full_label()]], testcase.get_step_combinations(1).iter().map(|it| it.iter().map(|t| t.get_full_label()).collect::<Vec<String>>()).collect::<Vec<Vec<String>>>());
        assert_eq!(vec![vec![step_init.get_full_label(), step_a_in.get_full_label()], vec![step_a_in.get_full_label(), step_a_out.get_full_label()], vec![step_a_out.get_full_label(), step_c_out.get_full_label()]], testcase.get_step_combinations(2).iter().map(|it| it.iter().map(|t| t.get_full_label()).collect::<Vec<String>>()).collect::<Vec<Vec<String>>>());
        assert_eq!(vec![vec![step_init.get_full_label(), step_a_in.get_full_label(), step_a_out.get_full_label()], vec![step_a_in.get_full_label(), step_a_out.get_full_label(), step_c_out.get_full_label()]], testcase.get_step_combinations(3).iter().map(|it| it.iter().map(|t| t.get_full_label()).collect::<Vec<String>>()).collect::<Vec<Vec<String>>>());
        assert_eq!(vec![vec![step_init.get_full_label(), step_a_in.get_full_label(), step_a_out.get_full_label(), step_c_out.get_full_label()]], testcase.get_step_combinations(4).iter().map(|it| it.iter().map(|t| t.get_full_label()).collect::<Vec<String>>()).collect::<Vec<Vec<String>>>());
        assert_eq!(vec![Vec::<String>::new()], testcase.get_step_combinations(5).iter().map(|it| it.iter().map(|t| t.get_full_label()).collect::<Vec<String>>()).collect::<Vec<Vec<String>>>());

        assert_eq!(vec![(step_init.get_full_label(), step_a_in.get_full_label()), (step_a_in.get_full_label(), step_a_out.get_full_label()), (step_a_out.get_full_label(), step_c_out.get_full_label())], testcase.get_step_tuples().iter().map(|it| (it.0.get_full_label(), it.1.get_full_label())).collect::<Vec<(String, String)>>());
    }
}