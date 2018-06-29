#![feature(plugin, custom_derive, iterator_flatten, decl_macro)]
#![plugin(rocket_codegen)]
extern crate rocket;
extern crate rocket_contrib;
extern crate serde;
#[macro_use]
extern crate serde_derive;
extern crate serde_json;
extern crate uuid;
extern crate chrono;
#[macro_use]
extern crate lazy_static;
#[macro_use]
extern crate prettytable;
extern crate indexmap;
extern crate rulinalg;
extern crate reqwest;
extern crate config;
#[macro_use]
extern crate derivative;
extern crate glob;

mod models;
mod data_receiving_controller;
mod test_analysis_controller;
mod storage_service;
mod analysis_service;
mod settings;
mod configuration_controller;

use rocket::Rocket;
use reqwest::{Client, Url};
use settings::Settings;
use reqwest::header::ContentType;
use std::thread;
use std::sync::Mutex;
use std::sync::MutexGuard;

lazy_static! {
    //storage for the settings singleton and the current progress
    static ref SETTINGS: Mutex<Settings> = Mutex::new(Settings::unwrap(Settings::new()));
    static ref PROGRESS: Mutex<f32> = Mutex::new(0.0 as f32);
}

pub fn get_settings<'a>() -> MutexGuard<'a, Settings> {
    return match SETTINGS.lock() {
        Ok(s) => s,
        Err(poisoned) => poisoned.into_inner()
    };
}

fn reset_progress() {
    *PROGRESS.lock().unwrap() = 0.0;
}

fn send_progress(progress: f32) {
    let pf = move ||{
        let mut p = PROGRESS.lock().unwrap();
        if progress > *p {
            print!("Send progress of {} to Atana. ", progress);
            let client = Client::new();
            let res = client.post(Url::parse(&get_settings().analysis.progress_endpoint).unwrap())
                .body(progress.to_string())
                .header(ContentType::json())
                .send();
            match res {
                Ok(response) => println!("Success: {:?}", response),
                Err(error) => println!("An error occurred: {:?}", error)
            };
            *p = progress;
        }
    };
    if get_settings().analysis.use_thread_for_progress {
        thread::spawn(pf);
    }
    else{
        pf();
    }
}

fn create_rocket() -> Rocket {
    let mut rocket_instance = rocket::ignite();
    rocket_instance = data_receiving_controller::mount(rocket_instance);
    rocket_instance = test_analysis_controller::analysis_controller::mount(rocket_instance);
    rocket_instance = configuration_controller::mount(rocket_instance);
    rocket_instance
}

fn main() {
    create_rocket().launch();
}