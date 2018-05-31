package nl.utwente.axini.atana.controllers

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import nl.utwente.axini.atana.models.AnalysisResult
import nl.utwente.axini.atana.models.TestCase
import nl.utwente.axini.atana.models.TestResult
import nl.utwente.axini.atana.repository.AnalysisResultRepository
import nl.utwente.axini.atana.repository.TestLogsRepository
import nl.utwente.axini.atana.repository.TestModelRepository
import nl.utwente.axini.atana.repository.TestRunRepository
import nl.utwente.axini.atana.service.ConfigurationService
import nl.utwente.axini.atana.service.GroupingAndAnalysisService
import nl.utwente.axini.atana.validation.validate
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class AnalysisController(val testModelRepository: TestModelRepository, val testRunRepository: TestRunRepository, val configurationService: ConfigurationService,
						 val analysisResultRepository: AnalysisResultRepository, val groupingAndAnalysisService: List<GroupingAndAnalysisService>,
						 val testLogsRepository: TestLogsRepository) : AbstractController() {

	var trainingProgress: Float = 0f

	@PostMapping("analyse/send_data/{testRunId}")
	@ApiOperation("Trigger an action in Atana to send all the data of a specific test run id to the configured grouping and analysis service.")
	fun sendDataToAnalyser(@ApiParam("The test run id for which the data should all be gathered") @PathVariable("testRunId") testRunId: String) {
		validate(testRunId) //Generic validation of the data
		var model = when {
			isLong(testRunId) -> testModelRepository.findAllById(listOf(testRunId.toLong())).toList()
			isUUID(testRunId) -> testModelRepository.findAllByTestRunId(UUID.fromString(testRunId)).distinct()
			else -> throw InvalidRequestDataException("invalid test run id")
		}
		val coverageInformation = model.filter { it.testcaseId != null }
		model -= coverageInformation
		if (model.size > 1)
			throw IllegalStateException("There are too many different models with this test run id, please remove the wrong models or specify a database id as a long.")
		else if (model.isEmpty()) {
			throw IllegalStateException("There is no model with this test run id, please provide a proper model first")
		}
		groupingAndAnalysisServiceImpl.init()
		val allTraces = testRunRepository.findAllByTestRunId(model.first().testRunId!!).flatMap { it.testCases }.groupBy { it.verdict }
		if (log.isInfoEnabled && allTraces.containsKey(TestResult.UNKNOWN)) {
			val unknownTestCases = allTraces[TestResult.UNKNOWN]
			log.info("${unknownTestCases?.size} test cases found that did not pass or fail and that will not be send to the grouping and analysis service: $unknownTestCases")
		}
		analysisResultRepository.deleteAll()
		val passingTests = allTraces[TestResult.PASSED]?.toTypedArray() ?: arrayOf()
		val failingTests = allTraces[TestResult.FAILED]?.toTypedArray() ?: arrayOf()
		groupingAndAnalysisServiceImpl.submitAllData(model.first(), passingTests, failingTests, coverageInformation)
	}

	@PostMapping("analyse/testcase/{testRunId}/{testCaseIndex}")
	@ApiOperation("Trigger a test case to be analysed by the grouping an analysis service. This action returns the result")
	fun analyseTestCase(
			@ApiParam("The test run id that identifies the set of traces that should be analysed")
			@PathVariable("testRunId")
			testRunId: UUID,
			@ApiParam("The test case index that identifies the test to analyse from the set of traces. Use \'-1\' for all failing test cases.")
			@PathVariable("testCaseIndex")
			testCaseIndex: Int
	): List<AnalysisResult> {
		validate(testRunId, testCaseIndex) //Generic validation of the data
		if (!groupingAndAnalysisServiceImpl.isConfigured()) {
			throw IllegalStateException("Cannot analyse a test case without the service being configured yet. " +
					"Send the data to the service first, this will configure the service.")
		}
		val testTraces = testRunRepository.findAllByTestRunId(testRunId).toList()
		if (testTraces.size > 1)
			throw IllegalArgumentException("There are too many different test runs with this test run id, please remove the wrong test runs.")
		else if (testTraces.isEmpty()) {
			throw IllegalArgumentException("There is no test set with this test run id, please provide a proper test set first")
		}
		val testTrace = testTraces.first()
		var testLogs = testLogsRepository.findAllByTestRunId(testRunId).toList()
        if (testLogs.size > 1) {
            testLogs = testLogs.distinctBy { it.sutFilename }
            if (testLogs.size > 1)
                throw IllegalArgumentException("There are too many different test logs with this test run id, please remove the wrong test logs.")
        }else if (testLogs.isEmpty()) {
            throw IllegalArgumentException("There is no test log with this test run id, please provide a proper test log first")
        }
        val testLog = testLogs.first()
		trainingProgress = 0f //Reset the training progress counter
		if (testCaseIndex == -1) { //Analyse all failing tests
			return testTrace.testCases.filter { it.verdict == TestResult.FAILED }.map {
				val result = groupingAndAnalysisServiceImpl.submitTest(it)
				result.testRunId = testRunId
				result.testCaseIndex = it.caseindex
				result.sut_filename = testLog.sutFilename
				analysisResultRepository.save(result)
				return@map result
			}
		} else {
			val testcase: TestCase = testTrace.testCases.find { it.caseindex == testCaseIndex }
					?: throw InvalidRequestDataException("There is no test case with index $testCaseIndex.")
			val result = groupingAndAnalysisServiceImpl.submitTest(testcase)
			result.testRunId = testRunId
			result.testCaseIndex = testCaseIndex
			analysisResultRepository.save(result)
			return listOf(result)
		}
	}

	@PostMapping("/analyse/train/progress")
	@ApiOperation("", hidden = true)
	fun trainingProgress(@RequestBody progress: Float) {
		println("Progress update received: $progress")
		trainingProgress = progress
	}

	@GetMapping("/analyse/train/progress")
	@ApiOperation("Show the progress of the training (might not be up to date if the analysis service does not inform Atana)")
	fun trainingProgress(): Float {
		return trainingProgress
	}

	@GetMapping("/analyse/groups/{testRunId}")
	@ApiOperation("Show all the groups that have formed using the analysis")
	fun getAllGroups(@ApiParam("The test run id that identifies the run where the groups should be returned for")
					 @PathVariable("testRunId")
					 testRunId: UUID): Map<String, List<AnalysisResult>> {
		//Return the name of the group and the index of the test case
		return analysisResultRepository.findAllByTestRunId(testRunId).groupBy { it.groupName }.toMap()
	}

	val groupingAndAnalysisServiceImpl: GroupingAndAnalysisService
		get() {
			return groupingAndAnalysisService.find { it::class.simpleName?.split("$$")?.first() == configurationService.config.groupingAndAnalysisServiceImplementation.simpleName }
					?: throw IllegalStateException("No valid grouping and analysis service found")
		}
}