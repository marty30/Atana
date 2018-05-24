package nl.utwente.axini.atana.controllers

import nl.utwente.axini.atana.AtanaApplication
import nl.utwente.axini.atana.models.Configuration
import nl.utwente.axini.atana.service.GroupingAndAnalysisServiceDummyImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit4.SpringRunner
import java.net.URL

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [AtanaApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigurationControllerTest {
	@Autowired
	lateinit var testRestTemplate: TestRestTemplate

	@Test
	fun testEndpoint() {
		//Given
		val resultNoUrl = testRestTemplate.postForEntity("/config", mapOf("endpoint" to "This is not a valid url"), Any::class.java)
		val resultInvalidJson = testRestTemplate.postForEntity("/config", "This is not a valid data request", Any::class.java)
		val resultInvalidJsonValueType = testRestTemplate.postForEntity("/config", mapOf("unknown" to mapOf("key" to "data")), Any::class.java)
		val resultUnrecognizedProperty = testRestTemplate.postForEntity("/config", mapOf("unknown" to "data"), Any::class.java)
		val resultHttp = testRestTemplate.postForEntity("/config", mapOf("endpoint" to "http://localhost:8090/", "groupingAndAnalysisServiceImplementation" to "nl.utwente.axini.atana.service.GroupingAndAnalysisServiceDummyImpl"), Any::class.java)
		val resultHttps = testRestTemplate.postForEntity("/config", mapOf("endpoint" to "https://localhost:8090/", "groupingAndAnalysisServiceImplementation" to "nl.utwente.axini.atana.service.GroupingAndAnalysisServiceDummyImpl"), Any::class.java)

		//When
		val resultShow = testRestTemplate.getForEntity("/config", Configuration::class.java)

		//Then
		assertNotNull(resultNoUrl)
		assertEquals(HttpStatus.BAD_REQUEST, resultNoUrl?.statusCode)
		assertNotNull(resultInvalidJson)
		assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, resultInvalidJson?.statusCode)
		assertNotNull(resultInvalidJsonValueType)
		assertEquals(HttpStatus.BAD_REQUEST, resultInvalidJsonValueType?.statusCode)
		assertNotNull(resultUnrecognizedProperty)
		assertEquals(HttpStatus.BAD_REQUEST, resultUnrecognizedProperty?.statusCode)
		assertNotNull(resultHttp)
		assertEquals(HttpStatus.OK, resultHttp?.statusCode)
		assertNotNull(resultHttps)
		assertEquals(HttpStatus.OK, resultHttps?.statusCode)
		assertNotNull(resultShow)
		assertEquals(HttpStatus.OK, resultShow?.statusCode)
		assertEquals(resultShow.body, Configuration(URL("https://localhost:8090/"), GroupingAndAnalysisServiceDummyImpl::class))
	}
}