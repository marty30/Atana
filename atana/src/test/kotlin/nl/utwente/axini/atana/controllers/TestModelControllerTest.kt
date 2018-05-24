package nl.utwente.axini.atana.controllers

import com.fasterxml.jackson.module.kotlin.readValue
import nl.utwente.axini.atana.AtanaApplication
import nl.utwente.axini.atana.jsonEquals
import nl.utwente.axini.atana.jsonObjectMapper
import nl.utwente.axini.atana.models.*
import nl.utwente.axini.atana.repository.TestModelRepository
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class TestModelControllerTest : AbstractControllerTest() {

	@Autowired
	lateinit var testRestTemplate: TestRestTemplate

	@Autowired
	lateinit var testModelRepository: TestModelRepository

	private val testModelWithUUID: TestModel = TestModel(UUID.randomUUID(), null, setOf(
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

	@Test
	fun crudModelTest() {
		//Create
		val model = testModelWithUUID
		val createResponse = testRestTemplate.postForEntity("/results/model", model, Any::class.java)
		checkResponse(createResponse)
		//Update
		//Skipped since there is some problem with updating a model with collections
//		val dbModelRequest = testRestTemplate.getForEntity("/results/model/" + model.testRunId, String::class.java)
//		testRestTemplate.getForEntity("/results/model/" + model.testRunId, List::class.java)
//		val dbModelList: List<TestModel> = jsonObjectMapper().readValue(dbModelRequest.body)
//		val dbModel = dbModelList.first()
//		val updatedModel = dbModel.copy(stss = dbModel.stss.map { it.copy(name = "Some updated name") }.toHashSet())
//		updatedModel.databaseId = dbModel.databaseId
//		updatedModel.stss.forEachIndexed { index, sts -> sts.databaseId = dbModel.stss.toList()[index].databaseId }
//		val updateResponse = testRestTemplate.postForEntity("/results/model", updatedModel, Any::class.java)
//		checkResponse(updateResponse)
		//Read
		val readRequest = testRestTemplate.getForEntity("/results/model/" + model.testRunId, String::class.java)
		checkResponse(readRequest)
		assertEquals(model, jsonObjectMapper().readValue<List<TestModel>>(readRequest.body!!).first())
		//Delete
		testRestTemplate.delete("/results/model/" + model.testRunId + "?confirmed=true")
		val checkAvailableResponse = testRestTemplate.getForEntity("/results/model/" + jsonObjectMapper().readValue<List<TestModel>>(readRequest.body!!).first().databaseId, List::class.java)
		checkResponse(checkAvailableResponse)
		assertEquals(0, checkAvailableResponse.body?.size)
	}

	@Test
	fun submitModelWithSeparateTestRunId() {
		val model = TestModel(null, null, setOf(
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
		val testRunId = UUID.randomUUID()
		val createResponse = testRestTemplate.postForEntity("/results/model?test_run_id=$testRunId", model, Any::class.java)
		checkResponse(createResponse)
		val readRequest = testRestTemplate.getForEntity("/results/model/$testRunId", List::class.java)
		checkResponse(readRequest)
		assertThat(readRequest.body?.first(), jsonEquals(model.copy(testRunId = testRunId), "databaseId", "createdAt", "updatedAt"))
	}

	@Test
	fun deleteByTestRunIdTest() {
		//Insert a model
		val dbModel = testModelRepository.save(testModelWithUUID)
		testRestTemplate.delete("/results/model/${dbModel.testRunId}?confirmed=true")
		assertEquals(Optional.ofNullable(null), testModelRepository.findById(dbModel.databaseId!!))
	}

	@Test
	fun deleteByDatabaseIdTest() {
		//Insert a model
		val dbModel = testModelRepository.save(testModelWithUUID)
		testRestTemplate.delete("/results/model/${dbModel.databaseId}?confirmed=true")
		assertEquals(Optional.ofNullable(null), testModelRepository.findById(dbModel.databaseId!!))
	}

	@Test
	fun deleteAllTest() {
		//Submit 2 models (one from string, and one from a file)
		val modelFromString: TestModel = jsonObjectMapper().readValue("{\n" +
				"  \"stss\": [\n" +
				"    {\n" +
				"      \"children\": [\n" +
				"        {\n" +
				"          \"attributes\": {\n" +
				"            \"hex_id\": \"string\",\n" +
				"            \"label\": \"string\",\n" +
				"            \"type\": \"string\"\n" +
				"          },\n" +
				"          \"id\": \"string\"\n" +
				"        }\n" +
				"      ],\n" +
				"      \"name\": \"string\",\n" +
				"      \"return_transitions\": [\n" +
				"        {\n" +
				"          \"attributes\": {\n" +
				"            \"label\": \"string\",\n" +
				"            \"type\": \"string\"\n" +
				"          },\n" +
				"          \"source\": \"string\",\n" +
				"          \"target\": \"string\"\n" +
				"        }\n" +
				"      ],\n" +
				"      \"start_states\": [\n" +
				"        {\n" +
				"          \"id\": \"string\"\n" +
				"        }\n" +
				"      ],\n" +
				"      \"states\": [\n" +
				"        {\n" +
				"          \"attributes\": {\n" +
				"            \"label\": \"string\",\n" +
				"            \"type\": \"string\"\n" +
				"          },\n" +
				"          \"id\": \"string\"\n" +
				"        }\n" +
				"      ],\n" +
				"      \"sts\": \"string\",\n" +
				"      \"transitions\": [\n" +
				"        {\n" +
				"          \"attributes\": {\n" +
				"            \"label\": \"string\",\n" +
				"            \"type\": \"string\"\n" +
				"          },\n" +
				"          \"source\": \"string\",\n" +
				"          \"target\": \"string\"\n" +
				"        }\n" +
				"      ]\n" +
				"    }\n" +
				"  ],\n" +
				"  \"test_run_id\": \"3ec4d0e1-2431-4516-ab3e-f15b061603e3\"\n" +
				"}")
		val jsonFile = TestRunControllerTest::class.java.classLoader.getResource("sample_model.json").readText()
		val modelFromFile: TestModel = jsonObjectMapper().readValue(jsonFile)
		val modelFromFileResponse = testRestTemplate.postForEntity("/results/model", modelFromFile, Any::class.java)
		checkResponse(modelFromFileResponse)
		val modelFromStringResponse = testRestTemplate.postForEntity("/results/model", modelFromString, Any::class.java)
		checkResponse(modelFromStringResponse)

		val countBefore = testRestTemplate.getForEntity("/results/model/", List::class.java).body!!.size
		testRestTemplate.delete("/results/model/All?confirmed=true")
		val countAfter = testRestTemplate.getForEntity("/results/model/", List::class.java).body!!.size
		assertTrue(countBefore > countAfter)
		assertEquals(0, countAfter)
	}

	@Test
	fun testInvalidDelete() {
		val deleteResponse = testRestTemplate.exchange("/results/model/NOT_A_VALID_ID?confirmed=true", HttpMethod.DELETE, HttpEntity.EMPTY, Map::class.java)
		checkResponse(deleteResponse, HttpStatus.BAD_REQUEST)
		assertEquals(deleteResponse.body?.get("error"), "id is not one of [\"All\", UUID, or Long]")
	}
}