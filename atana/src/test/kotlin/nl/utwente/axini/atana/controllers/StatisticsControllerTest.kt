package nl.utwente.axini.atana.controllers

import nl.utwente.axini.atana.AtanaApplication
import nl.utwente.axini.atana.models.*
import nl.utwente.axini.atana.repository.AnalysisResultRepository
import nl.utwente.axini.atana.repository.TestLogsRepository
import nl.utwente.axini.atana.repository.TestModelRepository
import nl.utwente.axini.atana.repository.TestRunRepository
import nl.utwente.axini.atana.transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
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
	fun showPassedTestRuns() {
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
		val analysisResultNow = AnalysisResult(modelNow.testRunId!!, 1, "group1", null, null, setOf())
		val testLogsNow = TestLogs(modelNow.testRunId!!, "", UUID.randomUUID(), "log 1", "log 2", "log 3")

		val yesterday = LocalDateTime.now().minusDays(2)
		val modelYesterday = TestModel(UUID.randomUUID(), null, setOf(Sts("TestStsYesterday", setOf(State("init", StateAttribute("init", "pos", null)), State("finish", StateAttribute("finish", "pos", null))), setOf(), setOf(StartState("init", null)), setOf(Transition("init", "finish", TransitionAttribute("go", null, null))), setOf(), null, null, null)), null)
		val testRunYesterday = TestRun(modelYesterday.testRunId!!, setOf(TestCase(1, null, TestResult.PASSED, "", setOf(Step(Label("init", "out", null), null, yesterday, setOf(), 1)), 1)))
		val analysisResultYesterday = AnalysisResult(modelYesterday.testRunId!!, 2, "group1", null, null, setOf())
		val testLogsYesterday = TestLogs(modelYesterday.testRunId!!, "", UUID.randomUUID(), "log 1", "log 2", "log 3")

		val beforeYesterday = LocalDateTime.now().minusDays(4)
		val modelBeforeYesterday = TestModel(UUID.randomUUID(), null, setOf(Sts("TestStsBeforeYesterday", setOf(State("init", StateAttribute("init", "pos", null)), State("finish", StateAttribute("finish", "pos", null))), setOf(), setOf(StartState("init", null)), setOf(Transition("init", "finish", TransitionAttribute("go", null, null))), setOf(), null, null, null)), null)
		val testRunBeforeYesterday = TestRun(modelBeforeYesterday.testRunId!!, setOf(TestCase(1, null, TestResult.PASSED, "", setOf(Step(Label("init", "out", null), null, beforeYesterday, setOf(), 1)), 1)))
		val analysisResultBeforeYesterday = AnalysisResult(modelBeforeYesterday.testRunId!!, 2, "group1", null, null, setOf())
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
		val analysisResultNow = AnalysisResult(modelNow.testRunId!!, 1, "group1", null, null, setOf())
		val testLogsNow = TestLogs(modelNow.testRunId!!, "", UUID.randomUUID(), "log 1", "log 2", "log 3")

		val yesterday = LocalDateTime.now().minusDays(2)
		val modelYesterday = TestModel(UUID.randomUUID(), null, setOf(Sts("TestSts", setOf(State("init", StateAttribute("init", "pos", null)), State("finish", StateAttribute("finish", "pos", null))), setOf(), setOf(StartState("init", null)), setOf(Transition("init", "finish", TransitionAttribute("go", null, null))), setOf(), null, null, null)), null)
		val testRunYesterday = TestRun(modelYesterday.testRunId!!, setOf(TestCase(1, null, TestResult.PASSED, "", setOf(Step(Label("init", "out", null), null, yesterday, setOf(), 1)), 1)))
		val analysisResultYesterday = AnalysisResult(modelYesterday.testRunId!!, 2, "group1", null, null, setOf())
		val testLogsYesterday = TestLogs(modelYesterday.testRunId!!, "", UUID.randomUUID(), "log 1", "log 2", "log 3")

		val beforeYesterday = LocalDateTime.now().minusDays(4)
		val modelBeforeYesterday = TestModel(UUID.randomUUID(), null, setOf(Sts("TestSts", setOf(State("init", StateAttribute("init", "pos", null)), State("finish", StateAttribute("finish", "pos", null))), setOf(), setOf(StartState("init", null)), setOf(Transition("init", "finish", TransitionAttribute("go", null, null))), setOf(), null, null, null)), null)
		val testRunBeforeYesterday = TestRun(modelBeforeYesterday.testRunId!!, setOf(TestCase(1, null, TestResult.PASSED, "", setOf(Step(Label("init", "out", null), null, beforeYesterday, setOf(), 1)), 1)))
		val analysisResultBeforeYesterday = AnalysisResult(modelBeforeYesterday.testRunId!!, 2, "group1", null, null, setOf())
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
