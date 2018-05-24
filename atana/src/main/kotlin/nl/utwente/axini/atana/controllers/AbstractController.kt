package nl.utwente.axini.atana.controllers

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import nl.utwente.axini.atana.logger
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import java.io.IOException
import java.net.MalformedURLException
import java.util.*

abstract class AbstractController {
	open val log by logger()

	fun isLong(string: String): Boolean {
		return try {
			string.toLong()
			true
		} catch (e: NumberFormatException) {
			false
		}
	}

	fun isUUID(string: String): Boolean {
		return try {
			UUID.fromString(string)
			true
		} catch (e: IllegalArgumentException) {
			false
		}
	}

	/**
	 * Handles all default exceptions by logging the exception and returning the error message
	 */
	@ExceptionHandler(
			MissingKotlinParameterException::class,
			NumberFormatException::class,
			InvalidRequestDataException::class,
			MalformedURLException::class,
			IllegalArgumentException::class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	fun defaultExceptionHandler(ex: Exception): Map<String, String?> {
		log.warn("A caught exception occurred: ${ex.message}", ex)
		return mapOf("error" to ex.message)
	}
}

class InvalidRequestDataException(msg: String) : IOException(msg)
