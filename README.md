# Atana

Atana is a toolset created to help in my master thesis. See the [full thesis here](http://purl.utwente.nl/essays/75676)

Contents:
- What is Atana and what does it do?
- Dependencies
- Getting started
- Docker images

## What is Atana and what does it do?
Atana is created to help in analysing test traces created by model based testing.
Atana is collection of tools that is used in my research project. Atana consists of a central service that collects the data required. 
This service is called Atana. The Atana service works with the SFL analysis service to analyse the collected data with the Spectrum-based Fault Localisation algorithm.
The Atana service also works with the data mining analysis service, which uses Weka to implement the different data mining techniques used in the analysis.


## Dependencies
Atana is created with a major dependency on [Axini's TestManager](http://axini.com). Axini TestManager is the model based testing tool that was used to generate the data.
Atana could likely work with other MBT tools, but was not created that way. The data used in the research is made available in the Thesis document.

Other dependencies for building and running Atana are:
- JVM
- Maven 3
- Rust Nightly
- Cargo

## Getting started
To get started modifying the Atana toolset, some steps need to be taken.
- First of all, the dependencies must be satisfied. Install the java development kit 8 (or higher), install maven 3, install Rust+Cargo through [Rustup](https://rustup.rs/) and finally select the nightly Rust version (`rustup install nightly`).
- Secondly the Atana-models should be installed by going to the atana-models directory and running mvn install (`cd atana-models && mvn install`).
- After this, Atana can be build. Docker images can also be build immediately by executing the docker-plugin: `mvn package docker:build`
- To start Atana, the AtanaApplication class can be run. Atana will start by default on port 8080. Visiting localhost:8080 will redirect to the Swagger UI interface that contains all documentation of the endpoints.
- Atana needs to be configured with a JSON document to fully function. An example configuration using the SFL service is shown below.
- To run and build the SFL service, simply running `cargo run` is sufficient. This will start the SFL service on localhost:8000.
- The data mining service works in a similar way to Atana, just run `mvn package docker:build` and then start the DMApplication class
- Finally Atana can be managed in Swagger UI.

To analyse any data the data must be supplied first to the appropriate endpoints.

```
{
    "endpoint": "http://sfl:8000/data",
    "groupingAndAnalysisServiceImplementation": "nl.utwente.axini.atana.service.GroupingAndAnalysisServiceRestImpl",
    "groupingAndAnalysisServiceConfiguration": {
        "progress_endpoint": "http://atana:8080/analyse/train/progress",
        "use_thread_for_training": "false",
        "similarity_threshold": 0.85,
        "number_of_pairs_to_include_for_order": 1,
        "use_steps_instead_of_transitions_for_analysis": "false",
        "use_transition_data": "true",
        "return_highest_similarity_if_nothing_found": "false"
    }
}
```

## Docker images
If you do not want to develop, but only use Atana and the related services, different docker images are available on the docker hub. These images are used in the docker-full-stack folder in this repo.
The following images are available:
- [Atana](https://hub.docker.com/r/marty30/atana/)
- [SFL service](https://hub.docker.com/r/marty30/sfl/)
- [DM service](https://hub.docker.com/r/marty30/dm_service/)
