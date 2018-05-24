extern crate rulinalg;

use models::model::TestModel;
use models::test_case::TestCase;
use std::sync::Mutex;
use std::sync::MutexGuard;
use models::coverage_matrix::CoverageMatrix;

lazy_static! {
    static ref STORAGE_SERVICE: Mutex<StorageService> = Mutex::new(StorageService::new());
}

pub fn get_storage_service<'a>() -> MutexGuard<'a, StorageService> {
    return match STORAGE_SERVICE.lock() {
        Ok(service) => service,
        Err(poisoned) => poisoned.into_inner()
    };
}

pub struct StorageService {
    pub model: Option<TestModel>,
    pub failing: Vec<TestCase>,
    pub passing: Vec<TestCase>,
    pub coverage_information: Vec<TestModel>,
    pub coverage_matrix: Option<CoverageMatrix>,
}

impl StorageService {
    pub fn new() -> StorageService {
        StorageService {
            model: None,
            failing: vec![],
            passing: vec![],
            coverage_information: vec![],
            coverage_matrix: None,
        }
    }

    pub fn set_model(&mut self, model: Option<TestModel>) {
        self.model = model;
    }

    pub fn set_passing_tests(&mut self, passing: Vec<TestCase>) {
        self.passing = passing;
    }

    pub fn set_failing_tests(&mut self, failing: Vec<TestCase>) {
        self.failing = failing;
    }

    pub fn set_coverage_informations(&mut self, coverage_information: Vec<TestModel>) {
        self.coverage_information = coverage_information;
    }

    pub fn set_coverage_matrix(&mut self, coverage_matrix: CoverageMatrix){
        self.coverage_matrix = Some(coverage_matrix);
    }

    pub fn reset(&mut self) {
        self.set_model(None);
        self.set_passing_tests(vec![]);
        self.set_failing_tests(vec![]);
    }
}