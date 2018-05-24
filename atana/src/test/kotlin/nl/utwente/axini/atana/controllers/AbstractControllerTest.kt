package nl.utwente.axini.atana.controllers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

abstract class AbstractControllerTest {
	fun checkResponse(response: ResponseEntity<out Any>, expectedHttpStatus: HttpStatus = HttpStatus.OK) {
		assertNotNull(response)
		assertEquals("Incorrect status code. The response: $response", expectedHttpStatus, response.statusCode)
	}
}