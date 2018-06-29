package nl.utwente.axini.controllers

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import nl.utwente.axini.atana.models.AnalysisResult
import nl.utwente.axini.atana.models.TestCase
import nl.utwente.axini.atana.models.TestModel
import nl.utwente.axini.model.Config
import nl.utwente.axini.model.ConversionMethod
import nl.utwente.axini.service.StorageService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import weka.core.Attribute
import weka.core.DenseInstance
import weka.core.UnsupportedAttributeTypeException
import java.util.concurrent.atomic.AtomicBoolean

@Controller
class AnalysisController(val storageService: StorageService) {

	val configured = AtomicBoolean(false)
	@GetMapping("/configured")
	@ResponseBody
	fun configured(): Boolean {
		return configured.get() && storageService.config != null
	}

	@PostMapping("/configure")
	@ResponseBody
	fun configure(@RequestBody data: Config) {
		require(data.isValid()) { "The configuration data is not valid." }
		storageService.config = data
		storageService.config?.classifierDelegate?.invalidate()
		configured.set(true)
	}

	@DeleteMapping("/clear")
	@ResponseBody
	fun clear() {
		storageService.addAllPassingTests(emptyList(), true)
		storageService.addAllFailingTests(emptyList(), true)
		storageService.model = null
	}

	@PostMapping("/model")
	@ResponseBody
	fun model(@RequestBody model: TestModel) {
		storageService.model = model
	}

	@PostMapping("/passing_test")
	@ResponseBody
	fun passingTest(@RequestBody test: TestCase) {
		storageService.addPassingTest(test)
	}

	@PostMapping("/passing_tests")
	@ResponseBody
	fun passingTests(@RequestBody test: List<TestCase>) {
		storageService.addAllPassingTests(test, true)
	}

	@PostMapping("/failing_test")
	@ResponseBody
	fun failingTest(@RequestBody test: TestCase) {
		storageService.addFailingTest(test)
	}

	@PostMapping("/failing_tests")
	@ResponseBody
	fun failingTests(@RequestBody test: List<TestCase>) {
		storageService.addAllFailingTests(test, true)
	}

	@PostMapping("/coverages")
	@ResponseBody
	fun coverages(@RequestBody test: List<TestModel>) {}

	@PostMapping("/coverage")
	@ResponseBody
	fun coverage(@RequestBody test: TestModel) {}

	@PostMapping("/done")
	@ResponseBody
	fun done() {
		//Start the training process
		if (storageService.config != null) {
			storageService.config?.classifierModel?.options = weka.core.Utils.splitOptions(storageService.config?.clustererOptions)
		}
		storageService.config?.classifierModel?.buildClusterer(storageService.instances)

		val rcaInstances = storageService.config?.unwrapRCATrainingFile()
		storageService.config?.RCAClassifierModel?.buildClassifier(rcaInstances)

		//Classify all test cases
		storageService.classifiedInstances = storageService.instances.map {
			it to (storageService.config?.classifierModel?.clusterInstance(it) ?: -1)
		}.toMap()
	}

	@PostMapping("/analyse")
	@ResponseBody
	fun analyse(@RequestBody test: TestCase): AnalysisResult {
		//Use the trained classifier to put this in a group
		val headerAttributes = storageService.instancesHeaders

		val instance = DenseInstance(headerAttributes.size)
		instance.setDataset(storageService.instances)
		headerAttributes.forEach { attribute ->
            //First handle the special attributes that require special treatment, like counting the number of steps
			if (attribute.name() in storageService.specialStringAttributes || attribute.name() in storageService.specialNumericAttributes) {
				when (attribute.name()) {
					"nr_of_steps" -> instance.setValue(attribute, test.steps.size.toDouble())
					"nr_of_reoccurring_steps" -> instance.setValue(attribute, test.steps.groupBy { it.fullLabel }.filter { it.value.size > 1 }.count().toDouble())
					"first_step" -> instance.setValue(attribute, test.steps.first().fullLabel)
					"last_step" -> instance.setValue(attribute, test.steps.last().fullLabel)
				}
			}
            //Then handle the regular attributes, which are steps in the trace. How they are handled is dependent on the configuration.
            else {
				when (storageService.config?.conversionMethod) {
					ConversionMethod.NEGATIVE_COUNT -> {
						val stepCount = test.steps.map { it.fullLabel }.filter { it == attribute.name() }.count()
						instance.setValue(attribute, stepCount.toDouble() * -1)
					}
					ConversionMethod.PRESENCE -> {
						instance.setValue(attribute, if (test.steps.map { it.fullLabel }.contains(attribute.name())) 1.0 else 0.0)
					}
					ConversionMethod.INDEX -> {
						instance.setValue(attribute, test.steps.map { it.fullLabel }.indexOf(attribute.name()).toDouble())
					}
				}
			}
		}

		val classification = storageService.classifiedInstances.entries.find { it.key.toDoubleArray()!!.contentEquals(instance.toDoubleArray()) }?.value
		val allInTheSameGroup = storageService.classifiedInstances.entries.filter { it.value == classification }.map { it.key.toDoubleArray().mapIndexed { index, d -> instance.attribute(index) to d }.filter { it.second < -0.0 } }
		val occurancesOfAttributes: MutableMap<Attribute, Int> = mutableMapOf()
		allInTheSameGroup.forEach {
			it.forEach {
				occurancesOfAttributes[it.first] = occurancesOfAttributes.getOrDefault(it.first, 0) + 1
			}
		}
		val maxOccurances = occurancesOfAttributes.values.max()
		val maxOccurredAttributes = occurancesOfAttributes.filter { it.value == maxOccurances }.map { it.key.name() }
		val problematicSteps = test.steps.filter { it.fullLabel in maxOccurredAttributes }.map { it.copy(label = it.label.copy(), labelParameters = HashMap(it.labelParameters), notes = HashSet(it.notes)) }
		return AnalysisResult(null, test.caseindex, null,"Group $classification", null, null, problematicSteps.toSet())
	}

	/**
	 * Handles all default exceptions by logging the exception and returning the error message
	 */
	@ExceptionHandler(
			MissingKotlinParameterException::class,
			NumberFormatException::class,
			IllegalArgumentException::class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	fun defaultExceptionHandler(ex: Exception): Map<String, String?> {
		println("A caught exception occurred: ${ex.message}")
		ex.printStackTrace()
		return mapOf("error" to ex.message)
	}

	@ExceptionHandler(
			UnsupportedAttributeTypeException::class
	)
	@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
	@ResponseBody
	fun unsupportedAttributeTypeExceptionHandler(ex: UnsupportedAttributeTypeException): Map<String, String?> {
		println("A caught exception occurred: ${ex.message}")
		ex.printStackTrace()
		return mapOf("error" to ex.message)
	}
}