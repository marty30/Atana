package nl.utwente.axini.atana.controllers

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Example
import io.swagger.annotations.ExampleProperty
import nl.utwente.axini.atana.logger
import nl.utwente.axini.atana.models.TestModel
import nl.utwente.axini.atana.repository.TestModelRepository
import nl.utwente.axini.atana.validation.validate
import nl.utwente.axini.atana.validation.validateData
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class TestModelController(val testModelRepository: TestModelRepository) : AbstractController() {
	override val log by logger()

	@PostMapping("/results/model")
	@ApiOperation("Submit model of a corresponding to a test run.")
	fun submitModel(
			@ApiParam("The test_run_id if it is not yet in the json body", required = false)
			@RequestParam("test_run_id", required = false)
			testRunId: UUID?,
			@ApiParam("The mutant-path if it is not yet in the json body", required = false)
			@RequestParam(required = false)
			mutant: String?,

			@ApiParam("A json object representing the Labelled Transition System")
			@RequestBody testModel: TestModel) {
		validate(testModel) //Generic validation of the data
		if (testRunId != null && testModel.testRunId == null) {
			log.debug("Setting test_run_id from query parameter")
			testModel.testRunId = testRunId
		}
		if (mutant != null && testModel.mutant == null) {
			log.debug("Setting mutant from query parameter")
			testModel.mutant = mutant
		}
		log.debug("Received a test model: %s".format(testModel))
		testModel.cleanup()
		testModelRepository.save(testModel)
	}

	@GetMapping("/results/model")
	@ApiOperation("Show all models")
	fun showModels(): Iterable<TestModel> = testModelRepository.findAll()

	@GetMapping("/results/model/{id}")
	@ApiOperation("Show all models that have an id")
	fun showModel(
			@ApiParam("The id of the model to be returned (either the database id as a long, or the test run id as a UUID)",
					examples = Example(ExampleProperty("1"), ExampleProperty("All")))
			@PathVariable("id") id: String
	): List<TestModel> {
		validate(id) //Generic validation of the data
		val models = when {
			isUUID(id) -> testModelRepository.findAllByTestRunId(UUID.fromString(id))
			isLong(id) -> testModelRepository.findAllById(listOf(id.toLong()))
			else -> throw IllegalArgumentException("id is not one of [UUID, or Long]")
		}
		return models as List<TestModel>
	}

	@DeleteMapping("/results/model/{id}")
	@ApiOperation("Delete all models that have an id")
	fun delete(
			@ApiParam("A boolean check to validate if the deletion is confirmed")
			@RequestParam("confirmed", defaultValue = "false") confirmed: Boolean,
			@ApiParam("The id of the model to be removed (either the database id as a long, or the test run id as a UUID), or \"All\" if everything should be removed",
					examples = Example(ExampleProperty("1"), ExampleProperty("All")))
			@PathVariable("id") id: String) {
		validate(confirmed, id) //Generic validation of the data
		validateData(confirmed) { "Please confirm your request" }
		when {
			id == "All" -> testModelRepository.deleteAll()
			isUUID(id) -> testModelRepository.deleteByTestRunId(UUID.fromString(id))
			isLong(id) -> testModelRepository.deleteById(id.toLong())
			else -> throw IllegalArgumentException("id is not one of [\"All\", UUID, or Long]")
		}
	}
}