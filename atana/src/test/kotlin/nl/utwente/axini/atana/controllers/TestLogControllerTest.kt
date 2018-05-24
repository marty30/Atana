package nl.utwente.axini.atana.controllers

import nl.utwente.axini.atana.AtanaApplication
import nl.utwente.axini.atana.models.TestLogs
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit4.SpringRunner
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [AtanaApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestLogControllerTest : AbstractControllerTest() {
	@Autowired
	lateinit var testRestTemplate: TestRestTemplate

	@Test
	fun testSubmitInvalidLog() {
		//Given
		//When
		val resultInvalidData = testRestTemplate.postForEntity("/results/log", mapOf("filename" to "file content"), Exception::class.java)
		//Then
		checkResponse(resultInvalidData, HttpStatus.BAD_REQUEST)
	}

	@Test
	fun testSubmitObjectLog() {
		//Given
		val testLogs = TestLogs(UUID.randomUUID(), null, UUID.randomUUID(), "cre cr 3 content", "cre err 3 content", "cre mux err 3 content")
		//When
		val resultObject = testRestTemplate.postForEntity("/results/log", testLogs, Unit::class.java)
		//Then
		checkResponse(resultObject)
	}

	@Test
	fun testSubmitMapLog() {
		//Given
		//When
		val resultValid = testRestTemplate.postForEntity("/results/log", mapOf(
				"test_run_id" to UUID.randomUUID(),
				"testset_id" to UUID.randomUUID(),
				"sut_filename" to "some/path/to/sut",
				"cre_err3" to "file content that is valid",
				"cre_cr3" to "other file contents",
				"cre_mux_err3" to "even more file contents",
				"tm_result" to "this is the result"), Unit::class.java)
		//Then
		checkResponse(resultValid)
	}

	@Test
	fun testDeleteSingleLog() {
		val showLogs = testRestTemplate.getForEntity("/results/log", List::class.java)
		checkResponse(showLogs)
		if (showLogs.body?.isEmpty() == true) {
			//Submit a log first
			val submitFirstLog = testRestTemplate.postForEntity("/results/log", TestLogs(UUID.randomUUID(), null, UUID.randomUUID(), "first log", "second log", "third log"), Any::class.java)
			val submitSecondLog = testRestTemplate.postForEntity("/results/log", TestLogs(UUID.randomUUID(), null, UUID.randomUUID(), "first log", "second log", "third log"), Any::class.java)
			checkResponse(submitFirstLog)
			checkResponse(submitSecondLog)
		}

		val availableLogsResponse = testRestTemplate.getForEntity("/results/log", List::class.java)
		checkResponse(availableLogsResponse)
		val availableLogs: Array<LinkedHashMap<*, *>> = availableLogsResponse.body!!.map { it as LinkedHashMap<*, *> }.toTypedArray()
		testRestTemplate.delete("/results/log/${availableLogs.first()["test_run_id"]}?confirmed=true")
		testRestTemplate.delete("/results/log/${availableLogs.last()["databaseId"]}?confirmed=true")
		val availableLogsAfterDeletion = testRestTemplate.getForEntity("/results/log", List::class.java)
		assertThat(availableLogsAfterDeletion.body!!.map { it as Map<*, *> }.map { it["test_run_id"] }, not(contains(availableLogs.first()["test_run_id"])))
		assertThat(availableLogsAfterDeletion.body!!.map { it as Map<*, *> }.map { it["databaseId"] }, not(contains(availableLogs.last()["databaseId"])))
	}

	@Test
	fun testDeleteAllLogs() {
		val showLogs = testRestTemplate.getForEntity("/results/log", List::class.java)
		checkResponse(showLogs)
		if (showLogs.body?.isEmpty() == true) {
			//Submit a log first
			val submitFirstLog = testRestTemplate.postForEntity("/results/log", TestLogs(UUID.randomUUID(), null, UUID.randomUUID(), "first log", "second log", "third log"), Any::class.java)
			val submitSecondLog = testRestTemplate.postForEntity("/results/log", TestLogs(UUID.randomUUID(), null, UUID.randomUUID(), "first log", "second log", "third log"), Any::class.java)
			checkResponse(submitFirstLog)
			checkResponse(submitSecondLog)
		}

		val availableLogsResponse = testRestTemplate.getForEntity("/results/log", List::class.java)
		checkResponse(availableLogsResponse)
		testRestTemplate.delete("/results/log/All?confirmed=true")
		assertEquals(0, testRestTemplate.getForEntity("/results/log", List::class.java).body?.size)
	}

	@Test
	fun testInvalidDelete() {
		val deleteResponse = testRestTemplate.exchange("/results/log/NOT_A_VALID_ID?confirmed=true", HttpMethod.DELETE, HttpEntity.EMPTY, Map::class.java)
		checkResponse(deleteResponse, HttpStatus.BAD_REQUEST)
		assertEquals(deleteResponse.body?.get("error"), "id is not one of [\"All\", UUID, or Long]. Found: NOT_A_VALID_ID")
	}
}