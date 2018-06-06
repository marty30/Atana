package nl.utwente.axini.atana.service

import nl.utwente.axini.atana.jsonObjectMapper
import nl.utwente.axini.atana.logger
import nl.utwente.axini.atana.models.AnalysisResult
import nl.utwente.axini.atana.models.TestCase
import nl.utwente.axini.atana.models.TestModel
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate

@Service
class GroupingAndAnalysisServiceRestImpl(val configurationService: ConfigurationService, val restTemplate: RestTemplate) : GroupingAndAnalysisService() {
	val log by logger()
	val MAX_TESTS_TO_SEND_AT_ONCE: Int = 10

	lateinit var restServicebaseurl: String

	lateinit var jsonHeader: HttpHeaders

	var configured = false

	override fun isConfigured(): Boolean {
		if (!configured) {
			return false
		}
		try {
			val analyseResponse = restTemplate.getForEntity("$restServicebaseurl/configured", Boolean::class.java)
			return analyseResponse.body ?: false && !configurationService.freshConfig.get()
		} catch (clienError: HttpClientErrorException) {
			throw IllegalArgumentException("$restServicebaseurl returned ${clienError.message}", clienError.mostSpecificCause)
		} catch (serverError: HttpServerErrorException) {
			throw IllegalStateException("$restServicebaseurl returned ${serverError.message}", serverError.mostSpecificCause)
		}
	}

	override fun configure() {
		jsonHeader = HttpHeaders()
		jsonHeader.contentType = MediaType.APPLICATION_JSON

		restServicebaseurl = configurationService.config.endpoint?.toString() ?: throw IllegalStateException("No endpoint for the rest service configured while still using the rest service. Please configure the endpoint first.")
		if (restServicebaseurl.endsWith("/")) {
			restServicebaseurl = restServicebaseurl.substring(0, restServicebaseurl.length - 1)
		}
		restTemplate.postForEntity("$restServicebaseurl/configure", HttpEntity<Map<String, String>>(configurationService.config.groupingAndAnalysisServiceConfiguration, jsonHeader), String::class.java)
		configured = true
		configurationService.freshConfig.set(false)
	}

	override fun submitAllData(model: TestModel, passingTests: Array<TestCase>, failingTests: Array<TestCase>, coverageInformation: List<TestModel>) {
		val responseClear = catchRemoteException { restTemplate.exchange("$restServicebaseurl/clear", HttpMethod.DELETE, HttpEntity<Void>(null, jsonHeader), String::class.java) }
		if (responseClear.statusCode != HttpStatus.OK) {
			throw Error(mapOf("reset" to responseClear).toString())
		}

		val responseModel = catchRemoteException { restTemplate.postForEntity("$restServicebaseurl/model", HttpEntity<TestModel>(model, jsonHeader), String::class.java) }

		var responsePassing: ResponseEntity<String> = ResponseEntity.ok("")
			passingTests.forEach {
				responsePassing = catchRemoteException { restTemplate.postForEntity("$restServicebaseurl/passing_test", HttpEntity<TestCase>(it, jsonHeader), String::class.java) }
				if (responsePassing.statusCode != HttpStatus.OK) {
					return@forEach
				}
			}

		var responseFailing: ResponseEntity<String> = ResponseEntity.ok("")
			failingTests.forEach {
				responseFailing = catchRemoteException { restTemplate.postForEntity("$restServicebaseurl/failing_test", HttpEntity<TestCase>(it, jsonHeader), String::class.java) }
				if (responseFailing.statusCode != HttpStatus.OK) {
					return@forEach
				}
			}

		var responseCoverage: ResponseEntity<String> = ResponseEntity.ok("")
		if (coverageInformation.size > MAX_TESTS_TO_SEND_AT_ONCE) {
			coverageInformation.forEach {
				responseCoverage = catchRemoteException { restTemplate.postForEntity("$restServicebaseurl/coverage", HttpEntity<TestModel>(it, jsonHeader), String::class.java) }
				if (responseCoverage.statusCode != HttpStatus.OK) {
					return@forEach
				}
			}
		} else {
			responseCoverage = catchRemoteException { restTemplate.postForEntity("$restServicebaseurl/coverages", HttpEntity<List<TestModel>>(coverageInformation, jsonHeader), String::class.java) }
		}


		if (!(responseModel.statusCode == HttpStatus.OK && responsePassing.statusCode == HttpStatus.OK && responseFailing.statusCode == HttpStatus.OK && responseCoverage.statusCode == HttpStatus.OK)) {
			throw Error(mapOf(
					"model" to responseModel,
					"failing" to responseFailing,
					"passing" to responsePassing,
					"coverage" to responseCoverage
			).toString())
		} else {
			val trainCall = catchRemoteException { restTemplate.postForEntity("$restServicebaseurl/done", HttpEntity<Void>(null, jsonHeader), String::class.java) }
			if (trainCall.statusCode != HttpStatus.OK) {
				throw Error(mapOf("train" to trainCall).toString())
			}
		}
	}

	private fun catchRemoteException(entity: () -> ResponseEntity<String>): ResponseEntity<String> {
		return try {
			entity.invoke()
		} catch (clienError: HttpClientErrorException) {
			var message: String? = clienError.statusText
			if (message == null) {
				message = clienError.responseBodyAsString
			}
			ResponseEntity("$restServicebaseurl returned $message", clienError.statusCode)
		} catch (serverError: HttpServerErrorException) {
			var message: String? = serverError.statusText
			if (message == null) {
				message = serverError.responseBodyAsString
			}
			ResponseEntity("$restServicebaseurl returned $message", serverError.statusCode)
		}
	}

	fun clean() {
		restTemplate.delete("$restServicebaseurl/clear")
	}

	override fun submitTest(run: TestCase): AnalysisResult {
		try {
			val analyseResponse = restTemplate.postForEntity("$restServicebaseurl/analyse", run, String::class.java)
			return jsonObjectMapper().readValue(analyseResponse.body, AnalysisResult::class.java)
		} catch (clienError: HttpClientErrorException) {
			throw IllegalArgumentException("$restServicebaseurl returned ${clienError.message}", clienError.mostSpecificCause)
		} catch (serverError: HttpServerErrorException) {
			throw IllegalStateException("$restServicebaseurl returned ${serverError.message}", serverError.mostSpecificCause)
		}
	}
}