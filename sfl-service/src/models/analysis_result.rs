extern crate rocket;
extern crate rocket_contrib;
extern crate serde;
extern crate serde_json;

use models::model::Transition;
use models::model::State;
use models::test_case::Step;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct AnalysisResult {
    #[serde(rename = "groupName")]
    group_name: String,
    #[serde(rename = "rootCauseState")]
    root_cause_state: Option<State>,
    #[serde(rename = "rootCauseTransition")]
    root_cause_transition: Option<Transition>,
    #[serde(rename = "rootCauseSteps")]
    pub root_cause_steps: Vec<Step>,
}

impl AnalysisResult {
    pub fn new(group_name: String, root_cause_state: Option<State>, root_cause_transition: Option<Transition>, root_cause_steps: Option<Vec<Step>>) -> AnalysisResult {
        let rcs = match root_cause_steps {
            Some(r) => r,
            None => vec![]
        };
        AnalysisResult {
            group_name,
            root_cause_state,
            root_cause_transition,
            root_cause_steps: rcs
        }
    }
}