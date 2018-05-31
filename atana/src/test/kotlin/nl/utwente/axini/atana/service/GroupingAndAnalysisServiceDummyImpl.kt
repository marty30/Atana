package nl.utwente.axini.atana.service

import nl.utwente.axini.atana.models.*
import org.hibernate.Hibernate
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
class GroupingAndAnalysisServiceDummyImpl : GroupingAndAnalysisService() {
	var configured: Boolean = false
	var nextAnalysisResultToReturn: AnalysisResult = AnalysisResult(null, 0, null,"State and transition fault", State("last state", StateAttribute("last state", "string", null)), null, setOf())
	//Some statistical variables
	var configureIsCalled: AtomicInteger = AtomicInteger()
	var submittedModel: TestModel? = null
	var submittedPassingTests: Array<TestCase>? = arrayOf()
	var submittedFailingTests: Array<TestCase>? = arrayOf()
	var submittedCoverageInformation: List<TestModel> = listOf()
	var lastSubmittedTestRun: TestCase? = null

	override fun isConfigured() = configured

	override fun configure() {
		configureIsCalled.incrementAndGet()
		configured = true
	}

	override fun submitAllData(model: TestModel, passingTests: Array<TestCase>, failingTests: Array<TestCase>, coverageInformation: List<TestModel>) {
		Hibernate.initialize(model.stss)
		submittedModel = model
		passingTests.forEach { Hibernate.initialize(it.steps) }
		submittedPassingTests = passingTests
		failingTests.forEach { Hibernate.initialize(it.steps) }
		submittedFailingTests = failingTests
		coverageInformation.forEach { Hibernate.initialize(it.stss) }
		submittedCoverageInformation = coverageInformation
	}

	override fun submitTest(run: TestCase): AnalysisResult {
		lastSubmittedTestRun = run
		return nextAnalysisResultToReturn
	}

	fun reset() {
		configureIsCalled.set(0)
		submittedModel = null
		submittedPassingTests = arrayOf()
		submittedFailingTests = arrayOf()
		lastSubmittedTestRun = null
	}
}