use models::model::TestModel;
use models::model::Transition;
use prettytable::cell::Cell;
use prettytable::row::Row;
use prettytable::Table;
use send_progress;
use std::collections::HashMap;
use std::vec::Vec;
use models::test_case::TestCase;
use models::test_case::Step;
use std::collections::HashSet;
use models::test_case::TestResult;

#[derive(Debug, Clone, Derivative)]
#[derivative(PartialEq, Eq)]
pub struct CoverageMatrix {
    pub error_vector: Vec<bool>, //Did the test at the index of this array pass?
    test_id_vector: Vec<i64>, //The id of the test.
    pub data: HashMap<String, Vec<bool>>, //The different steps and for each test if the step was covered by that test.
    number_of_tests: usize,
    number_of_steps: usize,
}

impl CoverageMatrix {
    pub fn new(error_vector: Vec<bool>, test_id_vector: Vec<i64>, data: HashMap<String, Vec<bool>>) -> CoverageMatrix {
        CoverageMatrix {
            number_of_tests: data.clone().values().next().unwrap_or(&vec![]).len(),
            number_of_steps: test_id_vector.clone().len(),
            error_vector,
            test_id_vector,
            data,
        }
    }

    pub fn to_coverage_table(&self) -> Table {
        let mut table = Table::new();
        { //Table header
            let mut table_row: Vec<Cell> = vec![Cell::new("id")];
            for step in self.data.keys() {
                table_row.push(Cell::new(&format!("{:?}", step)));
            }
            table_row.push(Cell::new("errored?"));
            table.add_row(Row::new(table_row));
        }


        //Transpose the data
        let mut matrix_values: Vec<Vec<bool>> = vec![];
        for i in 0..self.number_of_tests {
            let mut row = vec![];
            for (_, values) in self.data.iter() {
                assert_eq!(values.len(), self.number_of_tests);
                row.push(values[i]);
            }
            matrix_values.push(row);
        }

        for (i, (coverage_row, test_id)) in (matrix_values.iter()).zip(self.test_id_vector.iter()).enumerate() {
            let mut table_row: Vec<Cell> = vec![];
            table_row.push(Cell::new(&format!("T{}", test_id)));
            for val in coverage_row.iter() {
                table_row.push(Cell::new(&val.to_string()));
            }

            let relevant_error = self.error_vector.get(i).unwrap();

            table_row.push(Cell::new(&relevant_error.to_string()));
            table.add_row(Row::new(table_row));
        }

        table
    }

    pub fn from_traces(test_cases: Vec<TestCase>, steps_to_include: Option<Vec<Vec<Step>>>, min_progress: f32, max_progress: f32) -> CoverageMatrix {
        let send_progress_updates = max_progress > min_progress;
        let mut coverage_matrix: HashMap<String, Vec<bool>> = HashMap::new();
        let mut error_vector: Vec<bool> = vec![];
        let mut test_id_vector: Vec<i64> = vec![];
        if send_progress_updates { send_progress(min_progress); }

        match steps_to_include {
            Some(mut step_pairs) => {
                step_pairs.sort_by_key(|step_pair|step_pair.iter().map(|it| it.get_full_label()).collect::<Vec<_>>().join(" -> "));
                step_pairs.dedup_by_key(|step_pair|step_pair.iter().map(|it| it.get_full_label()).collect::<Vec<_>>().join(" -> "));
                for test in test_cases.iter() {
                    for step_pair in step_pairs.iter() {
                        let step_pair_covered = step_pair.iter().all(|s| test.steps.contains(s));
                        let string_pair = step_pair.iter().map(|it| it.get_full_label()).collect::<Vec<_>>().join(" -> ");
                        let mut cov_vec = coverage_matrix.entry(string_pair).or_insert(vec![]);
                        cov_vec.push(step_pair_covered);
                    }
                }
            },
            None => {
                let mut steps: HashSet<&Step> = HashSet::new();
                for test in test_cases.iter() {
                    for step in &test.steps {
                        steps.insert(step);
                    }
                }
                let mut step_labels: HashSet<String> = steps.iter().map(|it| it.get_full_label()).collect();

                for (i, test) in test_cases.iter().enumerate() {
                    error_vector.push((&test).verdict.as_ref().unwrap_or(&TestResult::unknown) != &TestResult::passed);
                    test_id_vector.push(test.id.unwrap_or(-1));

                    for step in step_labels.iter() {
                        let is_covered = test.steps.iter().find(|s| s.get_full_label() == *step).is_some();
                        let mut cov_vec = coverage_matrix.entry(step.to_string()).or_insert(vec![]);
                        cov_vec.push(is_covered);
                    }

                    if send_progress_updates { send_progress((i as f32 / test_cases.len() as f32) * (max_progress - min_progress) + min_progress); }
                }
            }
        }
        if send_progress_updates { send_progress(max_progress); }

        //Do some checks to validate the integrity of the data
        let number_of_tests = coverage_matrix.values().next().unwrap_or(&vec![]).len();
        let number_of_steps = coverage_matrix.keys().len();
        for tests in coverage_matrix.values() {
            assert_eq!(tests.len(), number_of_tests);
        }

        CoverageMatrix {
            error_vector,
            test_id_vector,
            data: coverage_matrix,
            number_of_tests,
            number_of_steps,
        }
    }

    pub fn from_model(val: Vec<TestModel>, transitions_to_include: Option<Vec<Vec<Transition>>>, min_progress: f32, max_progress: f32) -> CoverageMatrix {
        let send_progress_updates = max_progress > min_progress;
        let mut coverage_matrix: HashMap<String, Vec<bool>> = HashMap::new();
        let mut error_vector: Vec<bool> = vec![];
        let mut test_id_vector: Vec<i64> = vec![];
        if send_progress_updates { send_progress(min_progress); }
        match transitions_to_include {
            Some(mut transition_pairs) => {
                transition_pairs.sort_by_key(|transition_pair|transition_pair.iter().map(|it| it.to_string()).collect::<Vec<_>>().join(" "));
                transition_pairs.dedup_by_key(|transition_pair|transition_pair.iter().map(|it| it.to_string()).collect::<Vec<_>>().join(" "));
                for transition_pair in transition_pairs.iter() {
                    for test in val.iter() {
                        let transition_pair_covered = test.all_transitions().iter().filter(|transition| transition_pair.contains(transition)).map(|transition| transition.attributes.covered.unwrap_or(false))
                            .all(|cov| cov == true);
                        let string_pair = transition_pair.iter().map(|it| it.to_string()).collect::<Vec<_>>().join(" ");
                        let mut cov_vec = coverage_matrix.entry(string_pair).or_insert(vec![]);
                        cov_vec.push(transition_pair_covered);
                    }
                }
            },
            None => {
                for (i, test) in val.iter().enumerate() {
                    let mut passed = true;
                    for sts in &test.stss {
                        if let Some(ref props) = sts.trace_properties {
                            if let Some(props_passed) = props.passed {
                                passed = passed && props_passed
                            }
                        }

                        let mut relevant_transitions = (&sts).transitions.clone();
                        relevant_transitions.sort_by_key(|it|it.to_string());
                        relevant_transitions.dedup_by_key(|it|it.to_string());
                        for transition in relevant_transitions.iter() {
                            let is_covered = transition.attributes.covered.unwrap_or(false);
                            let mut cov_vec = coverage_matrix.entry(transition.to_string()).or_insert(vec![]);
                            cov_vec.push(is_covered);
                        }
                    }
                    error_vector.push(!passed);
                    test_id_vector.push(test.testcase_id.unwrap_or(-1));

                    if send_progress_updates { send_progress((i as f32 / val.len() as f32) * (max_progress - min_progress) + min_progress); }
                }
            }
        }
        if send_progress_updates { send_progress(max_progress); }

        //Do some checks to validate the integrity of the data
        let number_of_tests = coverage_matrix.values().next().unwrap_or(&vec![]).len();
        let number_of_steps = coverage_matrix.keys().len();
        for tests in coverage_matrix.values() {
            assert_eq!(tests.len(), number_of_tests);
        }

        CoverageMatrix {
            error_vector,
            test_id_vector,
            data: coverage_matrix,
            number_of_tests,
            number_of_steps,
        }
    }

    pub fn append(&self, to_append: CoverageMatrix) -> Result<CoverageMatrix, String> {
        //do some checks
        if to_append.number_of_tests != self.number_of_tests { return Err("number_of_tests was not equal for both coverage matrices".to_string()); }
        let mut new_data = HashMap::new();

        self.data.iter().chain(to_append.data.iter()).for_each(|entry| {
            new_data.insert(entry.0.to_string(), entry.1.clone());
        });
        return Ok(CoverageMatrix{
            number_of_tests: self.number_of_tests,
            number_of_steps: self.number_of_steps,
            test_id_vector: self.test_id_vector.clone(),
            error_vector: self.error_vector.clone(),
            data: new_data,
        });
    }
}

impl From<Vec<TestModel>> for CoverageMatrix {
    fn from(val: Vec<TestModel>) -> CoverageMatrix {
        CoverageMatrix::from_model(val, None, 0.0,0.0)
    }
}

impl From<Vec<TestCase>> for CoverageMatrix {
    fn from(val: Vec<TestCase>) -> CoverageMatrix {
        CoverageMatrix::from_traces(val, None, 0.0,0.0)
    }
}


#[cfg(test)]
pub mod tests {
    use models::coverage_matrix::CoverageMatrix;
    use std::collections::HashMap;

    #[test]
    fn test_append() {
        let error_vector = vec![true, false, false];
        let test_id_vector = vec![1, 5, 3];

        let mut first_data = HashMap::new();
        first_data.insert("a".to_string(), vec![true, false, true]);
        let first = CoverageMatrix::new(error_vector.clone(), test_id_vector.clone(), first_data);

        let mut second_data = HashMap::new();
        second_data.insert("b".to_string(), vec![true, false, false]);
        second_data.insert("c".to_string(), vec![true, true, false]);

        let second = CoverageMatrix::new(error_vector.clone(), test_id_vector.clone(), second_data);

        let mut expected_data = HashMap::new();
        expected_data.insert("a".to_string(), vec![true, false, true]);
        expected_data.insert("b".to_string(), vec![true, false, false]);
        expected_data.insert("c".to_string(), vec![true, true, false]);

        let expected = CoverageMatrix::new(error_vector, test_id_vector, expected_data);

        first.to_coverage_table().printstd();
        expected.to_coverage_table().printstd();

        assert_eq!(expected, first.append(second).unwrap());
    }
}