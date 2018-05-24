package nl.utwente.axini.atana.service

import com.fasterxml.jackson.module.kotlin.readValue
import nl.utwente.axini.atana.AtanaApplication
import nl.utwente.axini.atana.containsIgnoringCase
import nl.utwente.axini.atana.contentJson
import nl.utwente.axini.atana.jsonObjectMapper
import nl.utwente.axini.atana.models.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.ExpectedCount.*
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.*
import org.springframework.web.client.RestTemplate
import java.net.URL


@RunWith(SpringRunner::class)
@SpringBootTest(classes = [AtanaApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GroupingAndAnalysisServiceRestImplTest {
	@Autowired
	lateinit var configurationService: ConfigurationService

	@Autowired
	lateinit var groupingAndAnalysisService: GroupingAndAnalysisServiceRestImpl

	@Autowired
	lateinit var restTemplate: RestTemplate

	@LocalServerPort
	lateinit var port: String

	lateinit var server: MockRestServiceServer

	private final val modelString = ("{\n" +
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
	val modelFromString: TestModel = jsonObjectMapper().readValue(modelString)

	private final val testCaseString = ("{\n" +
			"      \"verdict\": \"error\",\n" +
			"      \"error_message\": \"Failed to observe results in adapter: Received ERROR: Could not connect to POS at ./tcp localhost 25801\",\n" +
			"      \"steps\": [\n" +
			"        {\n" +
			"          \"label\": {\n" +
			"            \"name\": \"error\",\n" +
			"            \"direction\": \"response\",\n" +
			"            \"channel\": \"error\"\n" +
			"          },\n" +
			"          \"timestamp\": \"2018-02-16T10:13:54.155+0100\",\n" +
			"          \"notes\": [],\n" +
			"          \"step_number\": 0,\n" +
			"          \"state_vector_size\": null,\n" +
			"          \"advance_duration_ms\": null,\n" +
			"          \"physical_label\": null,\n" +
			"          \"label_parameters\": {\n" +
			"          }\n" +
			"        }\n" +
			"      ],\n" +
			"      \"last_step\": 0,\n" +
			"      \"expected_labels\": [],\n" +
			"      \"tags\": []\n" +
			"    }")
	val testCaseFromString: TestCase = jsonObjectMapper().readValue(testCaseString)

	val failedTest = TestCase(-1, null, TestResult.FAILED, last_step = 2, steps = setOf())

	@Before
	fun setup() {
		server = MockRestServiceServer.createServer(restTemplate)
		val config = Configuration(
				endpoint = URL("http://localhost:8000/data"),
				groupingAndAnalysisServiceImplementation = GroupingAndAnalysisServiceRestImpl::class,
				groupingAndAnalysisServiceConfiguration = mapOf(
						"progress_endpoint" to "http://localhost:$port/analyse/train/progress",
						"use_thread_for_training" to "true",
						"similarity_threshold" to "0.75"
				)
		)
		configurationService.config = config
		groupingAndAnalysisService.configured = false
	}

	//A helper for initializing the service
	private fun initService() {
		server.expect(manyTimes(),
				requestTo("${configurationService.config.endpoint}/configure"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andRespond(withSuccess())
		server.expect(manyTimes(),
				requestTo("${configurationService.config.endpoint}/configured"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("true", MediaType.APPLICATION_JSON))
		groupingAndAnalysisService.init()
	}

	@Test
	fun testSubmittingAllData() {
		initService()
		server.reset()
		server.expect(times(2),
				requestTo("${configurationService.config.endpoint}/clear"))
				.andExpect(method(HttpMethod.DELETE))
				.andRespond(withSuccess())
		server.expect(once(),
				requestTo("${configurationService.config.endpoint}/model"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(contentJson<TestModel>(modelFromString))
				.andRespond(withSuccess())
		server.expect(once(),
				requestTo("${configurationService.config.endpoint}/passing_tests"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(contentJson<Array<TestCase>>(arrayOf(testCaseFromString)))
				.andRespond(withSuccess())
		server.expect(once(),
				requestTo("${configurationService.config.endpoint}/failing_tests"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(contentJson<Array<TestCase>>(arrayOf(failedTest)))
				.andRespond(withSuccess())
		server.expect(once(),
				requestTo("${configurationService.config.endpoint}/done"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess())

		groupingAndAnalysisService.submitAllData(modelFromString, arrayOf(testCaseFromString), arrayOf(failedTest), listOf())
		groupingAndAnalysisService.clean()

		server.verify()
	}

	@Test
	fun testSubmitInvalidData() {
		initService()
		//A test that has non 200 status codes (to receive an error map)
		server.reset()
		server.expect(times(2),
				requestTo("${configurationService.config.endpoint}/clear"))
				.andExpect(method(HttpMethod.DELETE))
				.andRespond(withSuccess())
		server.expect(once(),
				requestTo("${configurationService.config.endpoint}/model"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andRespond(withBadRequest().body(""))
		server.expect(once(),
				requestTo("${configurationService.config.endpoint}/passing_tests"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andRespond(withBadRequest().body(""))
		server.expect(once(),
				requestTo("${configurationService.config.endpoint}/failing_tests"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andRespond(withBadRequest().body(""))

		val ex = assertThrows<Error> { groupingAndAnalysisService.submitAllData(modelFromString, arrayOf(testCaseFromString), arrayOf(failedTest), listOf()) }
		assertThat(ex.message, containsString("model=<400"))
		assertThat(ex.message, containsString("passing=<400"))
		assertThat(ex.message, containsString("failing=<400"))
		groupingAndAnalysisService.clean()

		server.verify()
	}

	@Test
	fun testConfigure() {
		val groupingAndAnalysisServiceDummy = GroupingAndAnalysisServiceDummyImpl()
		groupingAndAnalysisServiceDummy.init()
		assertEquals(1, groupingAndAnalysisServiceDummy.configureIsCalled.get())
		//You should only configure once
		groupingAndAnalysisServiceDummy.init()
		assertEquals(1, groupingAndAnalysisServiceDummy.configureIsCalled.get())
	}

	@Test
	fun checkConfiguredBasedOnTheService() {
		server.reset()
		server.expect(once(),
				requestTo("${configurationService.config.endpoint}/configure"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(contentJson<Map<String, String>>(configurationService.config.groupingAndAnalysisServiceConfiguration))
				.andRespond(withSuccess())
		server.expect(once(),
				requestTo("${configurationService.config.endpoint}/configured"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("false", MediaType.APPLICATION_JSON))
		server.expect(once(),
				requestTo("${configurationService.config.endpoint}/configured"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("true", MediaType.APPLICATION_JSON))

		assertEquals(false, groupingAndAnalysisService.isConfigured())
		groupingAndAnalysisService.init()
		assertEquals(false, groupingAndAnalysisService.isConfigured())
		assertEquals(true, groupingAndAnalysisService.isConfigured())
		server.verify()
	}

	@Test
	fun testSubmitSingleTestWithSuccess() {
		initService()
		server.reset()

		val expectedResult = AnalysisResult(null, null, "State and transition fault", State("some id", StateAttribute("Some label", "Some type", null)), null, setOf())
		server.expect(once(),
				requestTo("${configurationService.config.endpoint}/analyse"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(contentJson<TestCase>(testCaseFromString))
				.andRespond(withSuccess().body(jsonObjectMapper().writeValueAsString(expectedResult)))
		val submissionResult = groupingAndAnalysisService.submitTest(testCaseFromString)
		assertEquals(expectedResult, submissionResult)
		server.verify()
	}

	@Test
	fun testSubmitSingleTestAndFailWithClientError() {
		initService()
		server.reset()
		val expectedResult = "Bad request"
		server.expect(once(),
				requestTo("${configurationService.config.endpoint}/analyse"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(contentJson<TestCase>(testCaseFromString))
				.andRespond(withBadRequest())
		val submissionResult = assertThrows<IllegalArgumentException> { groupingAndAnalysisService.submitTest(testCaseFromString) }
		assertThat(submissionResult.message, containsIgnoringCase("${configurationService.config.endpoint} returned"))
		assertThat(submissionResult.message, containsIgnoringCase(expectedResult))
		server.verify()
	}

	@Test
	fun testSubmitSingleTestAndFailWithServerError() {
		initService()
		server.reset()
		val expectedResult = "Server error"
		server.expect(once(),
				requestTo("${configurationService.config.endpoint}/analyse"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(contentJson<TestCase>(testCaseFromString))
				.andRespond(withServerError())
		val submissionResult = assertThrows<IllegalStateException> { groupingAndAnalysisService.submitTest(testCaseFromString) }
		assertThat(submissionResult.message, containsIgnoringCase("${configurationService.config.endpoint} returned"))
		assertThat(submissionResult.message, containsIgnoringCase(expectedResult))
		server.verify()
	}
}