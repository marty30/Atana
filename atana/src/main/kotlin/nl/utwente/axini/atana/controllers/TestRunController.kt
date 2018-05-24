package nl.utwente.axini.atana.controllers

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Example
import io.swagger.annotations.ExampleProperty
import nl.utwente.axini.atana.logger
import nl.utwente.axini.atana.models.TestModel
import nl.utwente.axini.atana.models.TestRun
import nl.utwente.axini.atana.repository.TestModelRepository
import nl.utwente.axini.atana.repository.TestRunRepository
import nl.utwente.axini.atana.validation.validate
import nl.utwente.axini.atana.validation.validateData
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class TestRunController(val testRunRepository: TestRunRepository, val testModelRepository: TestModelRepository) : AbstractController() {
	override val log by logger()

	@PostMapping("/results/run/trace")
	@ApiOperation("Submit data of a single test run")
	fun submitTestRun(
			@ApiParam("A json object with the test run")
			@RequestBody run: TestRun) {
		validate(run) //Generic validation of the data
		log.debug("Received data for a test run: %s".format(run))
		testRunRepository.save(run)
	}
	@PostMapping("/results/run/model")
	@ApiOperation("Submit data of a single test run")
	fun submitTestRunModel(
			@ApiParam("A json object with the test run")
			@RequestBody run: TestModel) {
		validate(run) //Generic validation of the data
		log.debug("Received data for a test run: %s".format(run))
		testModelRepository.save(run)
	}

	@GetMapping("/results/run")
	@ApiOperation("Show all test runs that are available")
	fun showTestRuns(): Iterable<TestRun> = testRunRepository.findAll()

	@GetMapping("/results/run/{id}")
	@ApiOperation("Show all test runs that are available")
	fun showTestRun(
			@ApiParam("The id of the test run to be returned (either the database id as a long, or the test run id as a UUID), or \"All\" if everything should be returned",
					examples = Example(ExampleProperty("1"), ExampleProperty("All")))
			@PathVariable("id") id: String): Iterable<TestRun> {
		validate(id) //Generic validation of the data
		return when {
			id == "All" -> testRunRepository.findAll()
			isUUID(id) -> testRunRepository.findAllByTestRunId(UUID.fromString(id))
			isLong(id) -> {
				val maybeRun = testRunRepository.findById(id.toLong())
				if (maybeRun.isPresent)
					listOf(maybeRun.get())
				else
					listOf()
			}
			else -> throw InvalidRequestDataException("id is not one of [\"All\", UUID, or Long]")
		}
	}

	@DeleteMapping("/results/run/{id}")
	@ApiOperation("Delete all test runs that have an id")
	fun deleteAll(
			@ApiParam("A boolean check to validate if the deletion is confirmed")
			@RequestParam("confirmed", defaultValue = "false") confirmed: Boolean,
			@ApiParam("The id of the test run to be removed (either the database id as a long, or the test run id as a UUID), or \"All\" if everything should be removed",
					examples = Example(ExampleProperty("1"), ExampleProperty("All")))
			@PathVariable("id") id: String) {
		validate(confirmed, id) //Generic validation of the data
		validateData(confirmed) { "Please confirm your request" }
		when {
			id == "All" -> testRunRepository.deleteAll()
			isUUID(id) -> testRunRepository.deleteByTestRunId(UUID.fromString(id))
			isLong(id) -> testRunRepository.deleteById(id.toLong())
			else -> throw InvalidRequestDataException("id is not one of [\"All\", UUID, or Long]")
		}
	}
}