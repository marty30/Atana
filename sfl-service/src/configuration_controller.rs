use rocket::Rocket;
use rocket_contrib::Json;
use serde_json::Map;
use serde_json::value::Value;
use settings::Analysis;
use std::sync::Mutex;

lazy_static! {
    static ref IS_CONFIGURED: Mutex<bool> = Mutex::new(false);
}

pub fn mount(rocket_instance: Rocket) -> Rocket {
    rocket_instance.mount("/data", routes![
        //This is a list of all controller functions that are mounted
        configure,
        configured,
        ])
}

#[post("/configure", format = "application/json", data = "<config>")]
fn configure(config: Json<Map<String, Value>>) {
    let config_map = config.into_inner();
    println!("Received config: {:?}", &config_map);

    Analysis::parse_from_map(config_map);

    if let Ok(mut x) = IS_CONFIGURED.lock() {
        *x = true
    }
}

#[get("/configured")]
fn configured() -> Json<bool> {
    match IS_CONFIGURED.lock() {
        Ok(x) => Json(*x),
        Err(_) => Json(false)
    }
}