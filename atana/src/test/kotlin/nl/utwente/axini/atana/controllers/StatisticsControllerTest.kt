package nl.utwente.axini.atana.controllers

import nl.utwente.axini.atana.AtanaApplication
import nl.utwente.axini.atana.models.*
import nl.utwente.axini.atana.repository.AnalysisResultRepository
import nl.utwente.axini.atana.repository.TestLogsRepository
import nl.utwente.axini.atana.repository.TestModelRepository
import nl.utwente.axini.atana.repository.TestRunRepository
import nl.utwente.axini.atana.transaction
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime
import java.util.*


@RunWith(SpringRunner::class)
@SpringBootTest(classes = [AtanaApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StatisticsControllerTest : AbstractControllerTest() {
	@Autowired
	lateinit var testRestTemplate: TestRestTemplate

	@Autowired
	lateinit var testModelRepository: TestModelRepository

	@Autowired
	lateinit var testRunRepository: TestRunRepository

	@Autowired
	lateinit var testLogsRepository: TestLogsRepository

	@Autowired
	lateinit var analysisResultRepository: AnalysisResultRepository

	@Test
	fun testShowStatistics() {
		lateinit var uuid: UUID
		for (i in 0..1) {
			val testrun1 = TestRun(UUID.randomUUID(), setOf(
					TestCase(1, 1, TestResult.PASSED, null, setOf(Step(Label("Init", "out", null), stepNumber = 1, timestamp = LocalDateTime.now())), 1),
					TestCase(2, 2, TestResult.FAILED, "Some error", setOf(Step(Label("Init", "out", null), stepNumber = 1, timestamp = LocalDateTime.now())), 1),
					TestCase(3, 3, TestResult.PASSED, null, setOf(Step(Label("Init", "out", null), stepNumber = 1, timestamp = LocalDateTime.now())), 1)
			))
			uuid = testrun1.testRunId
			testRunRepository.save(testrun1)
		}

		val converter = MappingJackson2HttpMessageConverter()
		converter.supportedMediaTypes = Arrays.asList(MediaType.APPLICATION_OCTET_STREAM)
		testRestTemplate.restTemplate.messageConverters.add(converter)

		val expectedResult = mapOf(
				"test_run_id" to uuid,
				"total_count" to 3,
				"passed_count" to 2,
				"failed_count" to 1,
				"passed_percentage" to 0.6666667,
				"failed_percentage" to 0.33333334
		)

		val response = testRestTemplate.exchange("/statistics", HttpMethod.GET, HttpEntity.EMPTY, List::class.java)
		checkResponse(response)
		assertThat(response.body?.find { (it as Map<*, *>)["test_run_id"] == uuid.toString() }?.toString(), equalTo(expectedResult.toString()))
	}

	@Test
	fun showFailedTestRuns() {
	}

	@Test
	fun getAllCompletelyPassedTestRuns() {
	}

	@Test
	fun getAllCompletelyFailedTestRuns() {
	}

	@Test
	@Category(RunSeparately::class) //"This test only works when it is ran on its own"
	fun deleteAllInTimerange() {
		testModelRepository.deleteAll()
		testLogsRepository.deleteAll()
		testRunRepository.deleteAll()
		analysisResultRepository.deleteAll()

		val now = LocalDateTime.now()
		val modelNow = TestModel(UUID.randomUUID(), null, setOf(Sts("TestStsNow", setOf(State("init", StateAttribute("init", "pos", null)), State("finish", StateAttribute("finish", "pos", null))), setOf(), setOf(StartState("init", null)), setOf(Transition("init", "finish", TransitionAttribute("go", null, null))), setOf(), null, null, null)), null)
		val testRunNow = TestRun(modelNow.testRunId!!, setOf(TestCase(1, null, TestResult.PASSED, "", setOf(Step(Label("init", "out", null), null, now, setOf(), 1)), 1)))
		val analysisResultNow = AnalysisResult(modelNow.testRunId!!, 1, null,"group1", null, null, setOf())
		val testLogsNow = TestLogs(modelNow.testRunId!!, "", UUID.randomUUID(), "log 1", "log 2", "log 3")

		val yesterday = LocalDateTime.now().minusDays(2)
		val modelYesterday = TestModel(UUID.randomUUID(), null, setOf(Sts("TestStsYesterday", setOf(State("init", StateAttribute("init", "pos", null)), State("finish", StateAttribute("finish", "pos", null))), setOf(), setOf(StartState("init", null)), setOf(Transition("init", "finish", TransitionAttribute("go", null, null))), setOf(), null, null, null)), null)
		val testRunYesterday = TestRun(modelYesterday.testRunId!!, setOf(TestCase(1, null, TestResult.PASSED, "", setOf(Step(Label("init", "out", null), null, yesterday, setOf(), 1)), 1)))
		val analysisResultYesterday = AnalysisResult(modelYesterday.testRunId!!, 2, null, "group1", null, null, setOf())
		val testLogsYesterday = TestLogs(modelYesterday.testRunId!!, "", UUID.randomUUID(), "log 1", "log 2", "log 3")

		val beforeYesterday = LocalDateTime.now().minusDays(4)
		val modelBeforeYesterday = TestModel(UUID.randomUUID(), null, setOf(Sts("TestStsBeforeYesterday", setOf(State("init", StateAttribute("init", "pos", null)), State("finish", StateAttribute("finish", "pos", null))), setOf(), setOf(StartState("init", null)), setOf(Transition("init", "finish", TransitionAttribute("go", null, null))), setOf(), null, null, null)), null)
		val testRunBeforeYesterday = TestRun(modelBeforeYesterday.testRunId!!, setOf(TestCase(1, null, TestResult.PASSED, "", setOf(Step(Label("init", "out", null), null, beforeYesterday, setOf(), 1)), 1)))
		val analysisResultBeforeYesterday = AnalysisResult(modelBeforeYesterday.testRunId!!, 2, null, "group1", null, null, setOf())
		val testLogsBeforeYesterday = TestLogs(modelBeforeYesterday.testRunId!!, "", UUID.randomUUID(), "log 1", "log 2", "log 3")

		testModelRepository.saveAll(listOf(modelNow, modelYesterday, modelBeforeYesterday))
		testLogsRepository.saveAll(listOf(testLogsNow, testLogsYesterday, testLogsBeforeYesterday))
		testRunRepository.saveAll(listOf(testRunNow, testRunYesterday, testRunBeforeYesterday))
		analysisResultRepository.saveAll(listOf(analysisResultNow, analysisResultYesterday, analysisResultBeforeYesterday))

		val response = testRestTemplate.exchange("/statistics/$yesterday/$now?confirmed=true", HttpMethod.DELETE, HttpEntity.EMPTY, String::class.java)
		checkResponse(response)
		transaction {
			val actual = testModelRepository.findAll().toList()
			assertEquals(listOf(modelBeforeYesterday), actual)
		}
	}

	@Test
	@Category(RunSeparately::class) //"This test only works when it is ran on its own"
	fun deleteAllInDaterange() {
		testModelRepository.deleteAll()
		testLogsRepository.deleteAll()
		testRunRepository.deleteAll()
		analysisResultRepository.deleteAll()

		val now = LocalDateTime.now()
		val modelNow = TestModel(UUID.randomUUID(), null, setOf(Sts("TestSts", setOf(State("init", StateAttribute("init", "pos", null)), State("finish", StateAttribute("finish", "pos", null))), setOf(), setOf(StartState("init", null)), setOf(Transition("init", "finish", TransitionAttribute("go", null, null))), setOf(), null, null, null)), null)
		val testRunNow = TestRun(modelNow.testRunId!!, setOf(TestCase(1, null, TestResult.PASSED, "", setOf(Step(Label("init", "out", null), null, now, setOf(), 1)), 1)))
		val analysisResultNow = AnalysisResult(modelNow.testRunId!!, 1, null, "group1", null, null, setOf())
		val testLogsNow = TestLogs(modelNow.testRunId!!, "", UUID.randomUUID(), "log 1", "log 2", "log 3")

		val yesterday = LocalDateTime.now().minusDays(2)
		val modelYesterday = TestModel(UUID.randomUUID(), null, setOf(Sts("TestSts", setOf(State("init", StateAttribute("init", "pos", null)), State("finish", StateAttribute("finish", "pos", null))), setOf(), setOf(StartState("init", null)), setOf(Transition("init", "finish", TransitionAttribute("go", null, null))), setOf(), null, null, null)), null)
		val testRunYesterday = TestRun(modelYesterday.testRunId!!, setOf(TestCase(1, null, TestResult.PASSED, "", setOf(Step(Label("init", "out", null), null, yesterday, setOf(), 1)), 1)))
		val analysisResultYesterday = AnalysisResult(modelYesterday.testRunId!!, 2, null, "group1", null, null, setOf())
		val testLogsYesterday = TestLogs(modelYesterday.testRunId!!, "", UUID.randomUUID(), "log 1", "log 2", "log 3")

		val beforeYesterday = LocalDateTime.now().minusDays(4)
		val modelBeforeYesterday = TestModel(UUID.randomUUID(), null, setOf(Sts("TestSts", setOf(State("init", StateAttribute("init", "pos", null)), State("finish", StateAttribute("finish", "pos", null))), setOf(), setOf(StartState("init", null)), setOf(Transition("init", "finish", TransitionAttribute("go", null, null))), setOf(), null, null, null)), null)
		val testRunBeforeYesterday = TestRun(modelBeforeYesterday.testRunId!!, setOf(TestCase(1, null, TestResult.PASSED, "", setOf(Step(Label("init", "out", null), null, beforeYesterday, setOf(), 1)), 1)))
		val analysisResultBeforeYesterday = AnalysisResult(modelBeforeYesterday.testRunId!!, 2, null, "group1", null, null, setOf())
		val testLogsBeforeYesterday = TestLogs(modelBeforeYesterday.testRunId!!, "", UUID.randomUUID(), "log 1", "log 2", "log 3")

		testModelRepository.saveAll(listOf(modelNow, modelYesterday, modelBeforeYesterday))
		testLogsRepository.saveAll(listOf(testLogsNow, testLogsYesterday, testLogsBeforeYesterday))
		testRunRepository.saveAll(listOf(testRunNow, testRunYesterday, testRunBeforeYesterday))
		analysisResultRepository.saveAll(listOf(analysisResultNow, analysisResultYesterday, analysisResultBeforeYesterday))

		val response = testRestTemplate.exchange("/statistics/${yesterday.toLocalDate()}/${now.toLocalDate()}?confirmed=true", HttpMethod.DELETE, HttpEntity.EMPTY, String::class.java)
		checkResponse(response)
		transaction {
			val actual = testModelRepository.findAll().toList()
			assertEquals(listOf(modelBeforeYesterday), actual)
		}
	}
}

class RunSeparately
