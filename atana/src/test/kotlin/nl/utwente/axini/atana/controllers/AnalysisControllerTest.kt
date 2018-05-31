package nl.utwente.axini.atana.controllers

import com.fasterxml.jackson.module.kotlin.readValue
import nl.utwente.axini.atana.AtanaApplication
import nl.utwente.axini.atana.jsonObjectMapper
import nl.utwente.axini.atana.models.*
import nl.utwente.axini.atana.repository.AnalysisResultRepository
import nl.utwente.axini.atana.repository.TestModelRepository
import nl.utwente.axini.atana.repository.TestRunRepository
import nl.utwente.axini.atana.service.ConfigurationService
import nl.utwente.axini.atana.service.GroupingAndAnalysisServiceDummyImpl
import org.hamcrest.Matchers.containsString
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [AtanaApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnalysisControllerTest : AbstractControllerTest() {
	@Autowired
	lateinit var testRestTemplate: TestRestTemplate

	@Autowired
	lateinit var configurationService: ConfigurationService

	@Autowired
	lateinit var testModelRepository: TestModelRepository

	@Autowired
	lateinit var testRunRepository: TestRunRepository

	@Autowired
	lateinit var analysisResultRepository: AnalysisResultRepository

	@Autowired
	lateinit var groupingAndAnalysisServiceDummyImpl: GroupingAndAnalysisServiceDummyImpl

	val testRunId = UUID.randomUUID()!!

	@Before
	fun setUp() {
		//Setup the configuration
		configurationService.config = Configuration(
				groupingAndAnalysisServiceImplementation = GroupingAndAnalysisServiceDummyImpl::class
		)
		groupingAndAnalysisServiceDummyImpl.reset()
	}

	@Test
	fun sendDataToAnalyser() {
		//Validate that it needs data first
		val sendDataActionResponseForEmptyRepository = testRestTemplate.postForEntity("/analyse/send_data/$testRunId", "", Any::class.java)
		checkResponse(sendDataActionResponseForEmptyRepository, HttpStatus.INTERNAL_SERVER_ERROR)

		//Submit some data to send
		val model = TestModel(testRunId, null, setOf(
				Sts("Some name",
						setOf(
								State("some state id", StateAttribute("some label", "State", null))
						),
						setOf(
								ChildModel("sme child model id", ChildModelAttribute("Some label", "some time", "some id", null))
						),
						setOf(
								StartState("some id", null)
						),
						setOf(
								Transition("some state id", "some state id", TransitionAttribute("Some label", null, null))
						),
						setOf(
								Transition("some state id", "some state id", TransitionAttribute("Some label", null, null))
						),
						"some state id",
						null, null)
		), null)

		val coverageModel = TestModel(testRunId, null, setOf(
				Sts("Some name",
						setOf(
								State("some state id", StateAttribute("some label", "State", true))
						),
						setOf(
								ChildModel("sme child model id", ChildModelAttribute("Some label", "some time", "some id", false))
						),
						setOf(
								StartState("some id", null)
						),
						setOf(
								Transition("some state id", "some state id", TransitionAttribute("Some label", null, true))
						),
						setOf(
								Transition("some state id", "some state id", TransitionAttribute("Some label", null, null))
						),
						"some state id",
						null, TraceProperties(setOf(), setOf(), true))
		), 1)

		val passingTestRun = TestRun(testRunId, setOf(TestCase(verdict = TestResult.PASSED, steps = setOf(Step(Label(name = "name", direction = "out", channel = "mux"), timestamp = LocalDateTime.now(), stepNumber = 1)), last_step = 1)))
		val failingTestRun = TestRun(testRunId, setOf(TestCase(verdict = TestResult.FAILED, steps = setOf(Step(Label(name = "name", direction = "out", channel = "mux"), timestamp = LocalDateTime.now(), stepNumber = 1)), last_step = 1)))
		val erroredTestRun = TestRun(testRunId, setOf(TestCase(verdict = TestResult.ERROR, steps = setOf(Step(Label(name = "name", direction = "out", channel = "mux"), timestamp = LocalDateTime.now(), stepNumber = 1)), last_step = 1)))
		val unknownTestRun = TestRun(testRunId, setOf(TestCase(verdict = TestResult.UNKNOWN, steps = setOf(Step(Label(name = "name", direction = "out", channel = "mux"), timestamp = LocalDateTime.now(), stepNumber = 1)), last_step = 1)))
		testModelRepository.save(coverageModel)
		testModelRepository.save(model)
		testRunRepository.saveAll(setOf(passingTestRun, failingTestRun, erroredTestRun, unknownTestRun))

		val sendDataActionResponse = testRestTemplate.postForEntity("/analyse/send_data/$testRunId", "", Any::class.java)
		checkResponse(sendDataActionResponse)

		assertEquals(model, groupingAndAnalysisServiceDummyImpl.submittedModel)
		assertArrayEquals(passingTestRun.testCases.toTypedArray(), groupingAndAnalysisServiceDummyImpl.submittedPassingTests)
		assertArrayEquals(failingTestRun.testCases.toTypedArray(), groupingAndAnalysisServiceDummyImpl.submittedFailingTests)
		assertArrayEquals(arrayOf(coverageModel), groupingAndAnalysisServiceDummyImpl.submittedCoverageInformation.toTypedArray())
//		assertThat(groupingAndAnalysisServiceDummyImpl.configureIsCalled.get(), greaterThan(0))
	}

	@Test
	fun analyseTestCase() {
		//Prepare the service
		groupingAndAnalysisServiceDummyImpl.init()

		//Validate a failure after using improper parameter types
		val invalidParameterResponse = testRestTemplate.postForEntity("/analyse/testcase/NOT_AN_ID/NOT_A_NUMBER", "", Any::class.java)
		checkResponse(invalidParameterResponse, HttpStatus.BAD_REQUEST)
		val negativeIndexResponse = testRestTemplate.postForEntity("/analyse/testcase/NOT_AN_ID/NOT_A_NUMBER", "", Any::class.java)
		checkResponse(negativeIndexResponse, HttpStatus.BAD_REQUEST)

		//Validate that we can only send existing test cases
		var nonExistingUuid = UUID.randomUUID()
		while (testRunRepository.findAllByTestRunId(nonExistingUuid).toSet().isNotEmpty()) {
			nonExistingUuid = UUID.randomUUID()
		}
		val notExistingResponse = testRestTemplate.postForEntity("/analyse/testcase/$nonExistingUuid/10", "", Map::class.java)
		checkResponse(notExistingResponse, HttpStatus.BAD_REQUEST)
		assertThat(notExistingResponse.body?.get("error").toString(), containsString("There is no test set with this test run id, please provide a proper test set first"))

		//Insert a test run
		val testRun = TestRun(UUID.randomUUID(), setOf(
				TestCase(1, null, TestResult.FAILED, null, setOf(Step(Label("Some label", "out", "stdout"), timestamp = LocalDateTime.now(), stepNumber = 1)), 1)
		))
		testRunRepository.save(testRun)

		//Validate that we can only send a single test case at the time
		val notTooManyResponse = testRestTemplate.postForEntity("/analyse/testcase/${testRun.testRunId}", "", Any::class.java)
		checkResponse(notTooManyResponse, HttpStatus.NOT_FOUND)

		//Validate submitting a test case  will send the whole test case
		val validResponse = testRestTemplate.postForEntity("/analyse/testcase/${testRun.testRunId}/${testRun.testCases.first().caseindex}", "", String::class.java)
		checkResponse(validResponse)
		assertEquals(testRun.testCases.first(), groupingAndAnalysisServiceDummyImpl.lastSubmittedTestRun)
		assertEquals(groupingAndAnalysisServiceDummyImpl.nextAnalysisResultToReturn, jsonObjectMapper().readValue<List<AnalysisResult>>(validResponse.body!!)[0])
	}

	@Test
	fun checkGroups() {
		//Given
		val testRunId = UUID.randomUUID()
		val model = TestModel(testRunId, null, setOf(Sts("TestSts", setOf(State("init", StateAttribute("init", "pos", null)), State("finish", StateAttribute("finish", "pos", null))), setOf(), setOf(StartState("init", null)), setOf(Transition("init", "finish", TransitionAttribute("go", null, null))), setOf(), null, null, null)), null)
		val analysisResult1 = AnalysisResult(testRunId, 1, null, "group1", null, null, setOf())
		val analysisResult2 = AnalysisResult(testRunId, 2, null, "group1", null, null, setOf())
		val analysisResult3 = AnalysisResult(testRunId, 3, null, "group2", null, null, setOf())
		val analysisResult4 = AnalysisResult(testRunId, 4, null, "group3", null, null, setOf())

		testModelRepository.deleteAll()
		testModelRepository.save(model)
		analysisResultRepository.deleteAll()
		analysisResultRepository.saveAll(listOf(analysisResult1, analysisResult2, analysisResult3, analysisResult4))


		//WHen
		val response = testRestTemplate.getForEntity("/analyse/groups/$testRunId", String::class.java)
		checkResponse(response)
		val groups: Map<String, List<AnalysisResult>> = jsonObjectMapper().readValue(response.body!!)

		//Then
		val expected: Map<String, List<AnalysisResult>> = mapOf(analysisResult1.groupName to listOf(analysisResult1, analysisResult2), analysisResult3.groupName to listOf(analysisResult3), analysisResult4.groupName to listOf(analysisResult4))
		assertEquals(expected, groups)

	}
}