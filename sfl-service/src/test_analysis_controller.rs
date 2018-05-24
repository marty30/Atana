extern crate rocket;
extern crate rocket_contrib;

pub mod analysis_controller {
    use models::test_case::TestCase;
    use rocket_contrib::Json;
    use rocket::Rocket;
    use models::analysis_result::AnalysisResult;
    use analysis_service::get_analysis_service;

    pub fn mount(rocket_instance: Rocket) -> Rocket {
        rocket_instance.mount("/data", routes![testcase])
    }

    #[post("/analyse", format = "application/json", data = "<testcase>")]
    pub fn testcase(testcase: Json<TestCase>) -> Json<AnalysisResult> {
        let service = get_analysis_service().unwrap();
        return Json(service.analyse(testcase.into_inner()).unwrap());
    }
}

extern crate chrono;

#[cfg(test)]
mod tests {
    use super::analysis_controller::*;
    use models::test_case::TestResult;
    use models::test_case::TestCase;
    use models::test_case::Step;
    use models::test_case::Label;
    use chrono::DateTime;
    use std::time::SystemTime;
    use models::model::State;
    use models::model::Transition;
    use models::model::StateAttribute;
    use models::model::TransitionAttribute;
    use models::analysis_result::AnalysisResult;
    use rocket_contrib::json::Json;
    use analysis_service::set_analysis_service;
    use analysis_service::tests::MockAnalysisService;
    use models::test_case::ExpectedLabel;

    pub static mut SERVICE_MOCK: MockAnalysisService = MockAnalysisService { next_result: None };

    #[test]
    fn check_testcase() {
        let test_testcase = {
            TestCase::new(
                Some(1),
                Some(TestResult::passed),
                None,
                vec![Step::new(
                    Label::new(
                        String::new(),
                        String::new(),
                        None,
                    ),
                    DateTime::from(SystemTime::now()),
                    None,
                    0,
                    None,
                    None,
                    None,
                    None,
                )],
                1,
                Some(vec![ExpectedLabel::new(Label::new(
                    String::new(),
                    String::new(),
                    None,
                ), Some(DateTime::from(SystemTime::now())))]),
                None,
            )
        };
        let expected_result = {
            AnalysisResult::new(
                "group".to_string(),
                Some(State::new("state 1".to_string(), StateAttribute::new("some label".to_string(), "string".to_string(), None))),
                Some(Transition::new("state 1".to_string(), "state 1".to_string(), TransitionAttribute::new("transition 1".to_string(), None, None))),
                None
            )
        };

        unsafe { //Unsafe because of static mutable variable SERVICE_MOCK. Because this is mutable, one thread could be updating SERVICE_MOCK while another is reading it, causing memory unsafety.
            set_analysis_service(&SERVICE_MOCK);
            SERVICE_MOCK.next_result = Some(expected_result.clone());
        }

        assert_eq!(testcase(Json(test_testcase)).into_inner(), expected_result);
    }
}