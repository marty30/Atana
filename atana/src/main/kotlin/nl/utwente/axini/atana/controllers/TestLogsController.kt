package nl.utwente.axini.atana.controllers

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Example
import io.swagger.annotations.ExampleProperty
import nl.utwente.axini.atana.logger
import nl.utwente.axini.atana.models.TestLogs
import nl.utwente.axini.atana.repository.TestLogsRepository
import nl.utwente.axini.atana.validation.validate
import nl.utwente.axini.atana.validation.validateData
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class TestLogsController(val testLogsRepository: TestLogsRepository) : AbstractController() {
	override val log by logger()

	@PostMapping("/results/log")
	@ApiOperation("Submit logs of a single test run.")
	fun submitLogs(
			@ApiParam("A json object with as key the name of the log file without extension and as value the contents of the log file")
			@RequestBody testLogs: TestLogs) {
		validate(testLogs) //Generic validation of the data
		log.debug("Received a set of logs: %s".format(testLogs))
		testLogsRepository.save(testLogs)
	}

	@GetMapping("/results/log")
	@ApiOperation("Show all available logs.")
	fun showLogs(): Iterable<TestLogs> = testLogsRepository.findAll()

	@GetMapping("/results/log/{id}")
	@ApiOperation("Show all available logs for test run id.")
	fun showLog(@ApiParam("The id of the log to be removed (either the database id as a long, or the test run id as a UUID), or \"All\" if everything should be removed",
			examples = Example(ExampleProperty("1"), ExampleProperty("All")))
				@PathVariable("id") id: String): Iterable<TestLogs> {
		validate(id) //Generic validation of the data
		return when {
			id == "All" -> testLogsRepository.findAll()
			isUUID(id) -> testLogsRepository.findAllByTestRunId(UUID.fromString(id))
			isLong(id) -> {
				val maybeLogs = testLogsRepository.findById(id.toLong())
				if (maybeLogs.isPresent)
					listOf(maybeLogs.get())
				else
					listOf()
			}
			else -> throw IllegalArgumentException("id is not one of [\"All\", UUID, or Long]. Found: $id")
		}
	}

	@DeleteMapping("/results/log/{id}")
	@ApiOperation("Delete the logs for a specific id or all logs that are available.")
	fun delete(
			@ApiParam("A boolean check to validate if the deletion is confirmed")
			@RequestParam("confirmed", defaultValue = "false") confirmed: Boolean,
			@ApiParam("The id of the log to be removed (either the database id as a long, or the test run id as a UUID), or \"All\" if everything should be removed",
					examples = Example(ExampleProperty("1"), ExampleProperty("All")))
			@PathVariable("id") id: String) {
		validate(confirmed, id) //Generic validation of the data
		validateData(confirmed) { "Please confirm your request" }
		when {
			id == "All" -> testLogsRepository.deleteAll()
			isUUID(id) -> testLogsRepository.deleteByTestRunId(UUID.fromString(id))
			isLong(id) -> testLogsRepository.deleteById(id.toLong())
			else -> throw IllegalArgumentException("id is not one of [\"All\", UUID, or Long]. Found: $id")
		}
	}
}