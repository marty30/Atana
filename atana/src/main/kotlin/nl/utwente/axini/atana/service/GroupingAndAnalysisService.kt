package nl.utwente.axini.atana.service

import nl.utwente.axini.atana.models.AnalysisResult
import nl.utwente.axini.atana.models.TestCase
import nl.utwente.axini.atana.models.TestModel

abstract class GroupingAndAnalysisService {
    fun init() {
        if (!isConfigured()) {
            configure()
        }
    }

    abstract fun isConfigured(): Boolean

    /**
     * Use this to configure the grouping and analysis service implementation (for example setting a response url for the external service)
     */
    protected open fun configure() {
        //By default, do nothing
    }

    /**
     * This method sends all data to the implementation that will be used to analyse the tests. After receiving this data, the service will automatically start training.
     *
     * @param model The model that is used for the test
     * @param passingTests The traces of all tests that have passed
     * @param failingTests The traces of all tests that have failed
     */
    abstract fun submitAllData(model: TestModel, passingTests: Array<TestCase>, failingTests: Array<TestCase>, coverageInformation: List<TestModel>)

    /**
     * Use this to analyse a single test run. This will return the group and the probable root cause as a state id from the model.
     */
    abstract fun submitTest(run: TestCase): AnalysisResult
}