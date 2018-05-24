package nl.utwente.axini.service

import com.fasterxml.jackson.module.kotlin.readValue
import nl.utwente.axini.atana.models.*
import nl.utwente.axini.controllers.AnalysisController
import nl.utwente.axini.jsonObjectMapper
import nl.utwente.axini.model.Config
import nl.utwente.axini.model.ConversionMethod
import org.junit.Test
import weka.clusterers.EM
import weka.core.Instance
import java.io.File
import java.time.LocalDateTime
import java.util.*

class ComparisonTest {

	val configNegativeCount = Config(EM::class.java.name, "", ConversionMethod.NEGATIVE_COUNT, "", "", "")
	val configPresence = Config(EM::class.java.name, "", ConversionMethod.PRESENCE, "", "", "")
	val configIndex = Config(EM::class.java.name, "", ConversionMethod.INDEX, "", "", "")

	@Test
	fun compareDifferentDataformattingMethods() {
		val model = TestModel(UUID.randomUUID(), null, setOf(
				Sts("example", setOf(
						State("state 0", StateAttribute("0", "node")),
						State("state 1", StateAttribute("1", "node")),
						State("state 2", StateAttribute("2", "node")),
						State("state 3a", StateAttribute("3a", "node")),
						State("state 3b", StateAttribute("3b", "node")),
						State("state 4", StateAttribute("4", "node")),
						State("state 5", StateAttribute("5", "node")),
						State("state 6", StateAttribute("6", "node")),
						State("unused", StateAttribute("unused", "node"))
				), setOf(), setOf(StartState("state 1")), setOf(
						Transition("state 0", "state 1", TransitionAttribute("!init", "out")),
						Transition("state 1", "state 2", TransitionAttribute("?a", "in")),
						Transition("state 1", "state 3a", TransitionAttribute("?b", "in")),
						Transition("state 3a", "state 3b", TransitionAttribute("", "tau")),
						Transition("state 2", "state 4", TransitionAttribute("!a", "out")),
						Transition("state 3b", "state 4", TransitionAttribute("!b", "out")),
						Transition("state 4", "state 5", TransitionAttribute("!c", "out")),
						Transition("state 4", "state 6", TransitionAttribute("!d", "out")),
						Transition("state 0", "unused", TransitionAttribute("", "tau"))
				), setOf(), null, null)
		))
		val testRun = TestRun(model.testRunId!!, setOf(
				TestCase(0, TestResult.PASSED, null, setOf(
						Step(Label("init", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("a", "in", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("a", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("c", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0)
				), 4),
				TestCase(1, TestResult.PASSED, null, setOf(
						Step(Label("init", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("a", "in", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("a", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("d", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0)
				), 4),
				TestCase(2, TestResult.FAILED, null, setOf(
						Step(Label("init", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("b", "in", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("b", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("c", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0)
				), 4),
				TestCase(3, TestResult.FAILED, null, setOf(
						Step(Label("init", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("b", "in", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("b", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("d", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0)
				), 4)
		))

		val (negativeCount, presence, index) = runComparison(model, testRun)

		println(negativeCount?.values)
		println(presence?.values)
		println(index?.values)

//		assert(negativeCount.values.toIntArray().contentEquals(presence.values.toIntArray()) && index.values.toIntArray().contentEquals(presence.values.toIntArray()))
	}

	@Test
	fun compareDifferentDataformattingMethods2() {
		val jsonFile = this::class.java.classLoader.getResource("test_model.json").readText()
		val modelFromFile: TestModel = jsonObjectMapper().readValue(jsonFile)

		val jsonTestRunFile = this::class.java.classLoader.getResource("test_run.json").readText()
		val testRunFromFile: TestRun = jsonObjectMapper().readValue(jsonTestRunFile)

		val (negativeCount, presence, index) = runComparison(modelFromFile, testRunFromFile)

		println(negativeCount?.values)
		println(presence?.values)
		println(index?.values)

//		assert(negativeCount.values.toIntArray().contentEquals(presence.values.toIntArray()) && index.values.toIntArray().contentEquals(presence.values.toIntArray()))
	}

	@Test
	fun compareDifferentDataformattingMethods3() {
		val jsonFile = this::class.java.classLoader.getResource("test_model2.json").readText()
		val modelFromFile: TestModel = jsonObjectMapper().readValue(jsonFile)

		val jsonTestRunFile = this::class.java.classLoader.getResource("test_run2.json").readText()
		val testRunFromFile: TestRun = jsonObjectMapper().readValue(jsonTestRunFile)

		val (negativeCount, presence, index) = runComparison(modelFromFile, testRunFromFile)

		println(negativeCount?.values)
		println(presence?.values)
		println(index?.values)

//		assert(negativeCount.values.toIntArray().contentEquals(presence.values.toIntArray()) && index.values.toIntArray().contentEquals(presence.values.toIntArray()))
	}

	@Test
	fun compareDifferentDataformattingMethodsFromDirectory() {
		val absoluteDirectoryPath = "/home/martijn/Axini/scrp/python_pos/bin/mutations"
		val mutants = File(absoluteDirectoryPath).list { _, s -> s.endsWith(".py") }
		val mutantDirs = mutants.map { File("$absoluteDirectoryPath/$it".substringBefore(".py")) }.filter { it.isDirectory }
		var numberEqual = 0
		var numberNotEqual = 0
		mutantDirs.forEach{ currentMutant ->
			println("-----------------$currentMutant--------------------")
			try {

				val jsonFiles = currentMutant.list { _, s -> s.endsWith(".json") }
				val model = File(currentMutant.absolutePath + "/model.json").readText()
				val modelFromFile: TestModel = jsonObjectMapper().readValue(model)
				val traces = File(currentMutant.absolutePath + "/" + jsonFiles.filterNot { it == "model.json" }.first()).readText()
				val testRunFromFile: TestRun = jsonObjectMapper().readValue(traces)

				val (negativeCount, presence, index) = runComparison(modelFromFile, testRunFromFile)


				println(negativeCount?.values)
				println(presence?.values)
				println(index?.values)
				val negIsPre = negativeCount?.values?.toIntArray()?.contentEquals(presence?.values?.toIntArray()
						?: IntArray(0))
				val preIsInd = presence?.values?.toIntArray()?.contentEquals(index?.values?.toIntArray() ?: IntArray(0))

				if (negIsPre == true && preIsInd == true) {
					numberEqual++
				} else if (negIsPre == false || preIsInd == false) {
					numberNotEqual++
				}
				println("negative == presence -> $negIsPre")
				println("presence == index -> $preIsInd")
			}
			catch (e: Exception){
				e.printStackTrace()
			}
			println("-------------------------------------------------------------------")
		}
		println("----------------------- Final statistics -------------------")
		println("number of equal: $numberEqual")
		println("number of inequal: $numberNotEqual")
	}

	private fun runComparison(model: TestModel, testRun: TestRun): Triple<Map<Instance, Int>?, Map<Instance, Int>?, Map<Instance, Int>?> {


		val passingTests = testRun.testCases.filter { it.verdict == TestResult.PASSED }
		val failingTests = testRun.testCases.filter { it.verdict == TestResult.FAILED }

		if (failingTests.isEmpty()) return Triple(null, null, null)

		val storageService = StorageService()
		val analysisController = AnalysisController(storageService)

		analysisController.clear()
		analysisController.model(model)
		analysisController.passingTests(passingTests)
		analysisController.failingTests(failingTests)

		analysisController.configure(configNegativeCount)

		analysisController.done()
		val negativeCount = storageService.classifiedInstances

		analysisController.clear()
		analysisController.model(model)
		analysisController.passingTests(passingTests)
		analysisController.failingTests(failingTests)

		analysisController.configure(configPresence)

		analysisController.done()
		val presence = storageService.classifiedInstances

		analysisController.clear()
		analysisController.model(model)
		analysisController.passingTests(passingTests)
		analysisController.failingTests(failingTests)

		analysisController.configure(configIndex)

		analysisController.done()
		val index = storageService.classifiedInstances

		return Triple(negativeCount, presence, index)
	}
}