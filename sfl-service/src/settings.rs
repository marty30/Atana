//Based on https://github.com/mehcode/config-rs/tree/master/examples/hierarchical-env

extern crate config;
extern crate serde_json;

use std::env;
use config::{ConfigError, Config, File, Environment};
use get_settings;
use serde_json::Map;
use serde_json::Value;

#[derive(Debug, Deserialize)]
pub struct Analysis {
    pub similarity_threshold: f32,
    pub progress_endpoint: String,
    pub use_thread_for_progress: bool,
    pub use_thread_for_training: bool,
    pub number_of_pairs_to_include_for_order: i32,
    pub use_steps_instead_of_transitions_for_analysis: bool,
    pub use_transition_data: bool,
    pub return_highest_similarity_if_nothing_found: bool,
}

impl Analysis {
    pub fn parse_from_map(settings_map: Map<String, Value>){
        println!("Parse map!");
        if settings_map.contains_key("similarity_threshold") {
            let similarity_threshold = Analysis::parse_f32(&settings_map["similarity_threshold"]);
            match similarity_threshold {
                Ok(similarity_threshold) => {
                    println!("Set similarity to {}", similarity_threshold);
                    get_settings().analysis.set_similarity_threshold(similarity_threshold);
                },
                Err(er) => println!("{}", er)
            }
        }
        if settings_map.contains_key("progress_endpoint") {
            let progress_endpoint = &settings_map["progress_endpoint"];
            match progress_endpoint {
                &Value::String(ref progress_endpoint) => {
                    println!("Set progress_endpoint to {}", progress_endpoint);
                    get_settings().analysis.set_progress_endpoint(progress_endpoint.to_string());
                },
                _ => println!("progress_endpoint is not a string: {}", progress_endpoint)
            }
        }
        if settings_map.contains_key("use_thread_for_training") {
            let use_thread_for_training = Analysis::parse_bool(&settings_map["use_thread_for_training"]);
            match use_thread_for_training {
                Ok(ref use_thread_for_training) => {
                    println!("Set use_thread_for_training to {}", use_thread_for_training);
                    get_settings().analysis.set_use_thread_for_training(*use_thread_for_training);
                },
                Err(e) => println!("use_thread_for_training is not a boolean: {}", e)
            }
        }
        if settings_map.contains_key("use_steps_instead_of_transitions_for_analysis") {
            let use_steps_instead_of_transitions_for_analysis = Analysis::parse_bool(&settings_map["use_steps_instead_of_transitions_for_analysis"]);
            match use_steps_instead_of_transitions_for_analysis {
                Ok(ref use_steps_instead_of_transitions_for_analysis) => {
                    println!("Set use_steps_instead_of_transitions_for_analysis to {}", use_steps_instead_of_transitions_for_analysis);
                    get_settings().analysis.use_steps_instead_of_transitions_for_analysis = *use_steps_instead_of_transitions_for_analysis;
                },
                Err(e) => println!("use_steps_instead_of_transitions_for_analysis is not a boolean: {}", e)
            }
        }
        if settings_map.contains_key("use_transition_data") {
            let use_transition_data = Analysis::parse_bool(&settings_map["use_transition_data"]);
            match use_transition_data {
                Ok(ref use_transition_data) => {
                    println!("Set use_transition_data to {}", use_transition_data);
                    get_settings().analysis.use_transition_data = *use_transition_data;
                },
                Err(e) => println!("use_transition_data is not a boolean: {}", e)
            }
        }
        if settings_map.contains_key("number_of_pairs_to_include_for_order") {
            let number_of_pairs_to_include_for_order = Analysis::parse_i32(&settings_map["number_of_pairs_to_include_for_order"]);
            match number_of_pairs_to_include_for_order {
                Ok(ref number_of_pairs_to_include_for_order) => {
                    println!("Set number_of_pairs_to_include_for_order to {}", number_of_pairs_to_include_for_order);
                    get_settings().analysis.number_of_pairs_to_include_for_order = *number_of_pairs_to_include_for_order;
                },
                Err(e) => println!("number_of_pairs_to_include_for_order is not an integer: {}", e)
            }
        }
        if settings_map.contains_key("return_highest_similarity_if_nothing_found") {
            let return_highest_similarity_if_nothing_found = Analysis::parse_bool(&settings_map["return_highest_similarity_if_nothing_found"]);
            match return_highest_similarity_if_nothing_found {
                Ok(ref return_highest_similarity_if_nothing_found) => {
                    println!("Set return_highest_similarity_if_nothing_found to {}", return_highest_similarity_if_nothing_found);
                    get_settings().analysis.return_highest_similarity_if_nothing_found = *return_highest_similarity_if_nothing_found;
                },
                Err(e) => println!("return_highest_similarity_if_nothing_found is not a boolean: {}", e)
            }
        }
    }

    fn parse_f32(val: &Value) -> Result<f32, String> {
        return match val {
            &Value::Number(ref value) => Ok(value.as_f64().unwrap() as f32),
            &Value::String(ref similarity_threshold_string) => match similarity_threshold_string.parse::<f32>() {
                Ok(s) => Ok(s),
                Err(e) => Err(e.to_string())
            },
            _ => Err(format!("No could not parse to f32. Found: {:?}.", val).to_string())
        }
    }
    fn parse_i32(val: &Value) -> Result<i32, String> {
        return match val {
            &Value::Number(ref value) => Ok(value.as_i64().unwrap() as i32),
            &Value::String(ref similarity_threshold_string) => match similarity_threshold_string.parse::<i32>() {
                Ok(s) => Ok(s),
                Err(e) => Err(e.to_string())
            },
            _ => Err(format!("No could not parse to i32. Found: {:?}.", val).to_string())
        }
    }

    fn parse_bool(val: &Value) -> Result<bool, String> {
        return match val {
            &Value::Bool(ref value) => Ok(*value),
            &Value::String(ref similarity_threshold_string) => match similarity_threshold_string.parse::<bool>() {
                Ok(s) => Ok(s),
                Err(e) => Err(e.to_string())
            },
            _ => Err("No similarity_threshold found.".to_string())
        }
    }

    pub fn set_similarity_threshold(&mut self, x: f32) {
        self.similarity_threshold = x;
    }
    pub fn set_progress_endpoint(&mut self, x: String) {
        self.progress_endpoint = x;
    }
    pub fn set_use_thread_for_training(&mut self, x: bool) {
        self.use_thread_for_training = x;
    }
}

#[derive(Debug, Deserialize)]
pub struct Settings {
    pub debug: bool,
    pub analysis: Analysis,
}

impl Settings {
    pub fn new() -> Result<Self, ConfigError> {
        let mut s = Config::new();

        // Start off by merging in the "default" configuration file
        s.merge(File::with_name("Settings.toml"))?;

        // Add in the current environment file
        // Default to 'development' env
        // Note that this file is _optional_
        let env = env::var("RUN_MODE").unwrap_or("development".into());
        s.merge(File::with_name(&format!("config/{}", env)).required(false))?;

        // Add in a local configuration file
        // This file shouldn't be checked in to git
        s.merge(File::with_name("config/local").required(false))?;

        // Add in settings from the environment (with a prefix of APP)
        // Eg.. `APP_DEBUG=1 ./target/app` would set the `debug` key
        s.merge(Environment::with_prefix("app"))?;

        // You can deserialize (and thus freeze) the entire configuration as
        s.try_into()
    }

    pub fn unwrap(wrapped: Result<Self, ConfigError>) -> Self {
        match wrapped {
            Ok(s) => s,
            Err(er) => {
                println!("{:?}", er);
                Settings {
                    debug: false,
                    analysis: Analysis {
                        similarity_threshold: 0.75,
                        progress_endpoint: String::from("http://localhost/analyse/train/progress"),
                        use_thread_for_progress: false,
                        use_thread_for_training: false,
                        number_of_pairs_to_include_for_order: 0,
                        use_steps_instead_of_transitions_for_analysis: false,
                        use_transition_data: false,
                        return_highest_similarity_if_nothing_found: false,
                    },
                }
            }
        }
    }
}