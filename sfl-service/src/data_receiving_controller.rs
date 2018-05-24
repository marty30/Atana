use models::model::TestModel;
use models::test_case::TestCase;
use rocket_contrib::Json;
use rocket::Rocket;
use storage_service::get_storage_service;
use analysis_service::get_analysis_service;
use std::thread;
use send_progress;
use get_settings;
use reset_progress;

pub fn mount(rocket_instance: Rocket) -> Rocket {
    rocket_instance.mount("/data", routes![
        //This is a list of all controller functions that are mounted
        index,
        model,
        show_model,
        passing_test,
        passing_tests,
        failing_test,
        failing_tests,
        coverage_information,
        coverage_information_list,
        inform_done,
        clear,
        ])
}

#[get("/")]
fn index() -> &'static str {
    "Use this entrypoint to submit data. To submit the model, use /model. \
    To submit passing traces, use /passing_tests. \
    To submit failing traces, use /failing_tests."
}

#[post("/model", format = "application/json", data = "<model>")]
fn model(model: Json<TestModel>) -> &'static str {
    get_storage_service().set_model(Some(model.into_inner()));
    return "Model received";
}

#[get("/model")]
fn show_model() -> Json<TestModel> {
    return match get_storage_service().model {
        Some(ref model) => Json(model.clone()),
        None => panic!("None found, so no model to show")
    };
}

#[post("/passing_tests", format = "application/json", data = "<passing_tests>")]
fn passing_tests(passing_tests: Json<Vec<TestCase>>) -> &'static str {
    get_storage_service().set_passing_tests(passing_tests.into_inner());
    return "passing_tests received";
}

#[post("/passing_test", format = "application/json", data = "<passing_test>")]
fn passing_test(passing_test: Json<TestCase>) -> &'static str {
    get_storage_service().passing.push(passing_test.into_inner());
    return "passing test received";
}


#[post("/failing_tests", format = "application/json", data = "<failing_tests>")]
fn failing_tests(failing_tests: Json<Vec<TestCase>>) -> &'static str {
    get_storage_service().set_failing_tests(failing_tests.into_inner());
    return "failing_tests received";
}

#[post("/failing_test", format = "application/json", data = "<failing_test>")]
fn failing_test(failing_test: Json<TestCase>) -> &'static str {
    get_storage_service().failing.push(failing_test.into_inner());
    return "failing test received";
}

#[post("/coverages", format = "application/json", data = "<coverage_information>")]
fn coverage_information_list(coverage_information: Json<Vec<TestModel>>) -> &'static str {
    get_storage_service().set_coverage_informations(coverage_information.into_inner());
    return "coverage_informations received";
}

#[post("/coverage", format = "application/json", data = "<coverage_information>")]
fn coverage_information(coverage_information: Json<TestModel>) -> &'static str {
    get_storage_service().coverage_information.push(coverage_information.into_inner());
    return "coverage information received";
}

#[post("/done", format = "application/json")]
fn inform_done() -> &'static str {
    let t = || {
        reset_progress();
        let analysis_service = get_analysis_service().unwrap();
        analysis_service.train();
        //Inform Atana that training is done
        send_progress(1.0) //100% done
    };
    if get_settings().analysis.use_thread_for_training {
        thread::spawn(t);
    } else {
        t();
    }
    return "Training started";
}

#[delete("/clear")]
fn clear() -> &'static str {
    get_storage_service().reset();
    return "everything cleared";
}


#[cfg(test)]
mod tests {
    use super::super::create_rocket;
    use super::*;
    use uuid::Uuid;
    use models::model::Sts;
    use std::panic::catch_unwind;
    use rocket::local::Client;
    use rocket::http::Status;
    use std::option::Option::Some;
    use std::option::Option::None;
    use models::test_case::TestResult;

    #[test]
    fn check_index() {
        let client = Client::new(create_rocket()).expect("valid rocket instance");
        let mut response = client.get("/data").dispatch();
        assert_eq!(response.status(), Status::Ok);
        assert_eq!(response.body_string(), Some("Use this entrypoint to submit data. To submit the model, use /model. To submit passing traces, use /passing_tests. To submit failing traces, use /failing_tests.".into()));
    }

    #[test]
    fn check_model() {
        let test_model = {
            TestModel::new(
                Uuid::new_v4(),
                vec![Sts::new(
                    String::new(),
                    vec![],
                    vec![],
                    vec![],
                    vec![],
                    vec![],
                    Some(String::new()),
                    Some(String::new()),
                    None,
                )], None)
        };

        get_storage_service().set_model(None);
        let empty_model_result = catch_unwind(|| show_model());
        assert!(empty_model_result.is_err());
        let model_response = model(Json(test_model.clone()));
        assert_eq!(model_response, "Model received");
        assert_eq!(show_model().into_inner(), test_model)
    }

    #[test]
    fn check_passing() {
        let passing_test = {
            TestCase::new(
                Some(1),
                Some(TestResult::passed),
                None,
                vec![],
                0,
                None,
                None)
        };

        let passing_response = passing_tests(Json(vec![passing_test.clone()]));
        assert_eq!(passing_response, "passing_tests received");
        assert!(get_storage_service().passing.contains(&passing_test));
    }

    #[test]
    fn check_failing() {
        let failing_test = {
            TestCase::new(
                Some(1),
                Some(TestResult::failed),
                Some("This is a test fail".to_string()),
                vec![],
                0,
                None,
                None)
        };

        let failing_response = failing_tests(Json(vec![failing_test.clone()]));
        assert_eq!(failing_response, "failing_tests received");
        assert!(get_storage_service().failing.contains(&failing_test));
    }

    #[test]
    fn check_reset() {
        //Given
        let test_model = {
            TestModel::new(
                Uuid::new_v4(),
                vec![Sts::new(
                    String::new(),
                    vec![],
                    vec![],
                    vec![],
                    vec![],
                    vec![],
                    Some(String::new()),
                    Some(String::new()),
                    None,
                )], None)
        };
        let passing_test = {
            TestCase::new(
                Some(1),
                Some(TestResult::passed),
                None,
                vec![],
                0,
                None,
                None)
        };
        let failing_test = {
            TestCase::new(
                Some(2),
                Some(TestResult::failed),
                Some("This is a test fail".to_string()),
                vec![],
                0,
                None,
                None)
        };

        get_storage_service().set_model(Some(test_model));
        get_storage_service().set_passing_tests(vec![passing_test]);
        get_storage_service().set_failing_tests(vec![failing_test]);

        //When
        let reset_response = clear();

        //Then
        assert_eq!(reset_response, "everything cleared");
        assert_eq!(get_storage_service().model, None);
        assert_eq!(get_storage_service().passing.len(), 0);
        assert_eq!(get_storage_service().failing.len(), 0);
    }
}

