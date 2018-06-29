use get_settings;
use models::test_case::Step;
use serde_json::Value;
use std::collections::HashMap;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize, Derivative)]
#[derivative(PartialEq)]
pub struct TestModel {
    pub test_run_id: Uuid,
    pub stss: Vec<Sts>,
    #[derivative(PartialEq = "ignore")]
    pub testcase_id: Option<i64>,
}

impl TestModel {
    pub fn new(test_run_id: Uuid, stss: Vec<Sts>, testcase_id: Option<i64>) -> TestModel {
        TestModel {
            test_run_id,
            stss,
            testcase_id,
        }
    }

    pub fn all_transitions(&self) -> Vec<Transition>{
        self.stss.iter().map(|it| it.transitions.clone()).flatten().collect::<Vec<_>>()
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, Derivative)]
#[derivative(PartialEq)]
pub struct Sts {
    name: String,
    pub states: Vec<State>,
    children: Vec<ChildModel>,
    start_states: Vec<StartState>,
    pub transitions: Vec<Transition>,
    return_transitions: Vec<Transition>,
    return_state: Option<String>,
    sts: Option<String>,
    #[derivative(PartialEq = "ignore")]
    pub trace_properties: Option<TraceProperties>,
}

impl Sts {
    pub fn new(name: String, states: Vec<State>, children: Vec<ChildModel>, start_states: Vec<StartState>, transitions: Vec<Transition>, return_transitions: Vec<Transition>, return_state: Option<String>, sts: Option<String>, trace_properties: Option<TraceProperties>) -> Sts {
        Sts {
            name,
            states,
            children,
            start_states,
            transitions,
            return_transitions,
            return_state,
            sts,
            trace_properties,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, Derivative)]
#[derivative(PartialEq)]
pub struct State {
    pub id: String,
    attributes: StateAttribute,
}

impl State {
    pub fn new(id: String, attributes: StateAttribute) -> State {
        State {
            id,
            attributes,
        }
    }

    pub fn to_start_state(&self) -> StartState {
        StartState { id: self.id.clone(), covered: self.attributes.covered.clone() }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, Derivative)]
#[derivative(PartialEq)]
pub struct StartState {
    id: String,
    #[derivative(PartialEq = "ignore")]
    covered: Option<bool>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Derivative)]
#[derivative(PartialEq)]
pub struct StateAttribute {
    label: String,
    #[serde(rename = "type")]
    _type: String,
    #[derivative(PartialEq = "ignore")]
    covered: Option<bool>,
}

impl StateAttribute {
    pub fn new(label: String, _type: String, covered: Option<bool>) -> StateAttribute {
        StateAttribute {
            label,
            _type,
            covered,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, Derivative)]
#[derivative(PartialEq)]
pub struct ChildModel {
    id: String,
    attributes: ChildModelAttribute,
}

#[derive(Debug, Clone, Serialize, Deserialize, Derivative)]
#[derivative(PartialEq)]
pub struct ChildModelAttribute {
    label: String,
    #[serde(rename = "type")]
    _type: String,
    hex_id: String,
    #[derivative(PartialEq = "ignore")]
    partially_covered: Option<bool>,
}

#[derive(Debug, Clone, Serialize, Deserialize, Eq, Hash, Derivative, Ord, PartialOrd)]
#[derivative(PartialEq)]
pub struct Transition {
    pub source: String,
    pub target: String,
    pub attributes: TransitionAttribute,
}

impl Transition {
    pub fn new(source: String, target: String, attributes: TransitionAttribute) -> Transition {
        Transition {
            source,
            target,
            attributes,
        }
    }

    pub fn clone_and_update<F>(&self, f: F) -> Self where
        Self: Sized, F: Fn(&mut Self) {
        let mut new_self = self.clone();
        f(&mut new_self);
        return new_self;
    }

    ///Create a string representation of the current transition. Depending on whether data should be included, the label consists of only the first word, or the full label.
    pub fn to_string(&self) -> String {
        if get_settings().analysis.use_transition_data {
            format!("{}--{}-->{}", self.source, self.attributes.label, self.target)
        } else {
            let dataless_label = self.attributes.label.split_whitespace().next();
            format!("{}--{}-->{}", self.source, dataless_label.unwrap_or("UNKNOWN TRANSITION LABEL"), self.target)
        }
    }

    //noinspection RsTypeCheck
    ///Convert the transition in a step, if possible, by finding the matching step based on as many parameters as possible.
    pub fn to_step(&self, steps: &Vec<Step>) -> Option<Step> {
        //First check if there is a full match
        for step in steps {
            if self.attributes.label == step.get_full_label_with_params(true) {
                return Some(step.clone());
            }
        }
        //If there is not a full match, check if there are steps that start with the transition's label.
        let mut alternatives = steps.iter().filter(|it|self.attributes.label.starts_with(&it.get_full_label())).collect::<Vec<&Step>>();
        alternatives.dedup();
        //Rank the alternatives based on how much the labels match (based on the label parameters)
        let alternatives_counted: HashMap<Step, usize> = alternatives.iter().map(|it|{
            let label = &self.attributes.label;
            let mut all_variables: Vec<String> = vec![];
            if let Some(ref label_parameters) = it.label_parameters {
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
        //Find the best (max) option and log it
        let result = alternatives_counted.iter().max_by(|first, second| first.1.cmp(&second.1));
        if result.is_some() {
            println!("No exactly matching step found for {:?}. Fallback to {:?}.", self.attributes.label, result);
        }
        else {
            println!("No exactly matching transition found for {:?}. No fallback found either.", self.attributes.label);
        }
        result.map(|it| it.0.clone())
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, Eq, Hash, Derivative, Ord, PartialOrd)]
#[derivative(PartialEq)]
pub struct TransitionAttribute {
    pub label: String,
    #[serde(rename = "type")]
    _type: Option<String>,
    #[derivative(PartialEq = "ignore")]
    pub covered: Option<bool>,
}

impl TransitionAttribute {
    pub fn new(label: String, _type: Option<String>, covered: Option<bool>) -> TransitionAttribute {
        TransitionAttribute {
            label,
            _type,
            covered,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, Derivative)]
#[derivative(PartialEq)]
pub struct TraceProperties {
    pub passed: Option<bool>,
}

impl TraceProperties {
    pub fn new(passed: Option<bool>) -> TraceProperties {
        TraceProperties {
            passed,
        }
    }
}