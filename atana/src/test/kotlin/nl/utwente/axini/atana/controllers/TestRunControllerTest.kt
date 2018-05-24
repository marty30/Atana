package nl.utwente.axini.atana.controllers

import nl.utwente.axini.atana.AtanaApplication
import nl.utwente.axini.atana.jsonObjectMapper
import nl.utwente.axini.atana.models.*
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [AtanaApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestRunControllerTest : AbstractControllerTest() {

	@Autowired
	lateinit var testRestTemplate: TestRestTemplate

	@Test
	fun testSubmitTracesJsonFile() {
		//Given
		val jsonFile = TestRunControllerTest::class.java.classLoader.getResource("sample_trace.json").readText()
		//When
		val headers = HttpHeaders()
		headers.contentType = MediaType.APPLICATION_JSON
		val resultValid = testRestTemplate.postForEntity("/results/run/trace", HttpEntity(jsonFile, headers), Any::class.java)
		//Then
		checkResponse(resultValid)
	}

	@Test
	fun testSubmitInvalidTraces() {
		//Given
		//When
		val resultInvalidData = testRestTemplate.postForEntity("/results/run/trace", mapOf("filename" to "file content"), Unit::class.java)
		//Then
		checkResponse(resultInvalidData, HttpStatus.BAD_REQUEST)
	}

	@Test
	fun testSubmitObjectTraces() {
		//Given
		val trace = TestRun(
				UUID.fromString("3ec4d0e1-2431-4516-ab3e-f15b061603e3"),
				setOf(TestCase(
						verdict = TestResult.PASSED, steps = setOf(
						Step(
								Label(
										name = "name",
										direction = "out",
										channel = "mux"
								),
								timestamp = LocalDateTime.now(),
								stepNumber = 1
						)
				), last_step = 1
				))
		)
		//When
		val headers = HttpHeaders()
		headers.contentType = MediaType.APPLICATION_JSON
		val resultValid = testRestTemplate.postForEntity("/results/run/trace", HttpEntity(jsonObjectMapper().writeValueAsString(trace), headers), Any::class.java)
		//Then
		checkResponse(resultValid)
	}

	@Test
	fun testDeleteSingleRun() {
		val showRuns = testRestTemplate.getForEntity("/results/run", List::class.java)
		checkResponse(showRuns)
		if (showRuns.body?.isEmpty() == true) {
			//Submit a run first
			val submitFirstRun = testRestTemplate.postForEntity("/results/run/trace", TestRun(UUID.randomUUID(), setOf(TestCase(-1, null, TestResult.PASSED, "", setOf(Step(Label("some name", "in", "stdout"), timestamp = LocalDateTime.now(), stepNumber = 1)), 1))), Any::class.java)
			val submitSecondRun = testRestTemplate.postForEntity("/results/run/trace", TestRun(UUID.randomUUID(), setOf(TestCase(-1, null, TestResult.FAILED, "Some error", setOf(Step(Label("some name", "in", "stdout"), timestamp = LocalDateTime.now(), stepNumber = 1)), 1))), Any::class.java)
			checkResponse(submitFirstRun)
			checkResponse(submitSecondRun)
		}

		val availableRunsResponse = testRestTemplate.getForEntity("/results/run", List::class.java)
		checkResponse(availableRunsResponse)
		val availableRuns: Array<LinkedHashMap<*, *>> = availableRunsResponse.body!!.map { it as LinkedHashMap<*, *> }.toTypedArray()
		MatcherAssert.assertThat(availableRuns.size, Matchers.greaterThanOrEqualTo(2))
		testRestTemplate.delete("/results/run/${availableRuns.first()["test_run_id"]}?confirmed=true")
		testRestTemplate.delete("/results/run/${availableRuns.last()["databaseId"]}?confirmed=true")
		val availableRunsAfterDeletion = testRestTemplate.getForEntity("/results/run", List::class.java)
		MatcherAssert.assertThat(availableRunsAfterDeletion.body!!.map { it as Map<*, *> }.map { it["test_run_id"] }, Matchers.not(Matchers.contains(availableRuns.first()["test_run_id"])))
		MatcherAssert.assertThat(availableRunsAfterDeletion.body!!.map { it as Map<*, *> }.map { it["databaseId"] }, Matchers.not(Matchers.contains(availableRuns.last()["databaseId"])))
	}

	@Test
	fun testDeleteAllRuns() {
		val showRuns = testRestTemplate.getForEntity("/results/run", List::class.java)
		checkResponse(showRuns)
		if (showRuns.body?.isEmpty() == true) {
			//Submit a run first
			val submitFirstRun = testRestTemplate.postForEntity("/results/run/trace", TestRun(UUID.randomUUID(), setOf()), Any::class.java)
			val submitSecondRun = testRestTemplate.postForEntity("/results/run/trace", TestRun(UUID.randomUUID(), setOf()), Any::class.java)
			checkResponse(submitFirstRun)
			checkResponse(submitSecondRun)
		}

		val availableRunsResponse = testRestTemplate.getForEntity("/results/run", List::class.java)
		checkResponse(availableRunsResponse)
		MatcherAssert.assertThat(availableRunsResponse.body!!.size, Matchers.greaterThanOrEqualTo(2))
		testRestTemplate.delete("/results/run/All?confirmed=true")
		Assert.assertEquals(0, testRestTemplate.getForEntity("/results/run", List::class.java).body?.size)
	}

	@Test
	fun testInvalidDelete() {
		val deleteResponse = testRestTemplate.exchange("/results/run/NOT_A_VALID_ID?confirmed=true", HttpMethod.DELETE, HttpEntity.EMPTY, Map::class.java)
		checkResponse(deleteResponse, HttpStatus.BAD_REQUEST)
		Assert.assertEquals(deleteResponse.body?.get("error"), "id is not one of [\"All\", UUID, or Long]")
	}
}