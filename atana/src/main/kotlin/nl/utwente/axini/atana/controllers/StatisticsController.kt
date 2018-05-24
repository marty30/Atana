package nl.utwente.axini.atana.controllers

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import nl.utwente.axini.atana.models.TestLogs
import nl.utwente.axini.atana.models.TestResult
import nl.utwente.axini.atana.repository.TestLogsRepository
import nl.utwente.axini.atana.repository.TestModelRepository
import nl.utwente.axini.atana.repository.TestRunRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.*

@RestController
class StatisticsController(val testRunRepository: TestRunRepository, val testLogsRepository: TestLogsRepository, val testModelRepository: TestModelRepository) : AbstractController() {
	@GetMapping("/statistics")
	@ApiOperation("Show all statistics")
	fun showStatistics(): List<MutableMap<String, Any>> {
		val testruns = testRunRepository.findAll().map {
			val stats = it.testCases.groupBy { it.verdict }
			return@map mutableMapOf(
					"test_run_id" to it.testRunId,
					"total_count" to it.testCases.size,
					"passed_count" to (stats[TestResult.PASSED]?.size ?: 0),
					"failed_count" to (stats[TestResult.FAILED]?.size ?: 0),
					"passed_percentage" to ((stats[TestResult.PASSED]?.size
							?: 0).toFloat() / it.testCases.size.toFloat()),
					"failed_percentage" to ((stats[TestResult.FAILED]?.size
							?: 0).toFloat() / it.testCases.size.toFloat())
			)
		}

		testruns.forEach {
			val testRunId: UUID = it["test_run_id"] as UUID
			val logs = testLogsRepository.findAllByTestRunId(testRunId).toList()
			if (logs.size > 1) {
				println("Multiple keys found: $logs")
			}
			it["test_set_id"] = logs[0].testsetId
			it["sut_filename"] = (logs[0].sutFilename ?: "")
		}

		return testruns
	}

	@GetMapping("/statistics/usable")
	@ApiOperation("Show tests that are usable")
	fun showUsableTestRuns(): List<MutableMap<String, Any>> {
		val minPercentage = 0.05
		val maxPercentage = 0.95

		return showStatistics().filter {
			it["failed_percentage"] as Float in minPercentage..maxPercentage &&
					it["passed_percentage"] as Float in minPercentage..maxPercentage
		}
	}

	@GetMapping("/statistics/unusable")
	@ApiOperation("Show tests that are NOT usable")
	fun showUnusableTestRuns(): List<MutableMap<String, Any>> {
		val minPercentage = 0.05
		val maxPercentage = 0.95

		return showStatistics().filter {
			it["failed_percentage"] as Float !in minPercentage..maxPercentage &&
					it["passed_percentage"] as Float !in minPercentage..maxPercentage
		}
	}

	@DeleteMapping("/statistics/unusable")
	@ApiOperation("Delete tests that are NOT usable")
	fun deleteUnusableTestRuns() {
		val minPercentage = 0.05
		val maxPercentage = 0.95

		showStatistics().filter {
			it["failed_percentage"] as Float !in minPercentage..maxPercentage &&
					it["passed_percentage"] as Float !in minPercentage..maxPercentage
		}.forEach {
			testLogsRepository.deleteByTestRunId(it["test_run_id"] as UUID)
			testModelRepository.deleteByTestRunId(it["test_run_id"] as UUID)
			testRunRepository.deleteByTestRunId(it["test_run_id"] as UUID)
		}
	}

	@GetMapping("/statistics/run/passed")
	@ApiOperation("Show all test logs of runs that have passed test cases")
	fun showPassedTestRuns(): List<TestLogs> {
		val testruns = testRunRepository.findAllByTestCases_Verdict(TestResult.PASSED)
		return testruns.map { testLogsRepository.findAllByTestRunId(it.testRunId) }.flatten().distinctBy { Triple(it.testRunId, it.sutFilename, it.testsetId) }
	}

	@GetMapping("/statistics/run/failed")
	@ApiOperation("Show all test logs of runs that have failed test cases")
	fun showFailedTestRuns(): List<TestLogs> {
		val testruns = testRunRepository.findAllByTestCases_Verdict(TestResult.FAILED)
		return testruns.map { testLogsRepository.findAllByTestRunId(it.testRunId) }.flatten().distinctBy { Triple(it.testRunId, it.sutFilename, it.testsetId) }
	}

	@GetMapping("/statistics/run/all_passed")
	@ApiOperation("Show all the test runs that have all passed")
	fun getAllCompletelyPassedTestRuns(): List<TestLogs> {
		//Return the name of the group and the index of the test case
		val passedTestruns = showPassedTestRuns()
		val failedTestruns = showFailedTestRuns()
		return passedTestruns.filterNot { failedTestruns.contains(it) }
	}

	@DeleteMapping("/statistics/run/all_passed")
	@ApiOperation("Delete all the test runs that have all passed")
	fun deleteAllCompletelyPassedTestRuns() {
		//Return the name of the group and the index of the test case
		val passedTestruns = showPassedTestRuns()
		val failedTestruns = showFailedTestRuns()
		val toDelete = passedTestruns.filterNot { failedTestruns.contains(it) }

		toDelete.forEach { testModelRepository.deleteByTestRunId(it.testRunId) }
		toDelete.forEach { testRunRepository.deleteByTestRunId(it.testRunId) }
		toDelete.forEach { testLogsRepository.deleteByTestRunId(it.testRunId) }
	}

	@GetMapping("/statistics/run/all_failed")
	@ApiOperation("Show all the test runs that have all failed")
	fun getAllCompletelyFailedTestRuns(): List<TestLogs> {
		//Return the name of the group and the index of the test case
		val passedTestruns = showPassedTestRuns()
		val failedTestruns = showFailedTestRuns()
		return failedTestruns.filterNot { passedTestruns.contains(it) }
	}

	@DeleteMapping("/statistics/run/all_failed")
	@ApiOperation("Delete all the test runs that have all failed")
	fun deleteAllCompletelyFailedTestRuns() {
		//Return the name of the group and the index of the test case
		val passedTestruns = showPassedTestRuns()
		val failedTestruns = showFailedTestRuns()
		val toDelete = failedTestruns.filterNot { passedTestruns.contains(it) }

		toDelete.forEach { testModelRepository.deleteByTestRunId(it.testRunId) }
		toDelete.forEach { testRunRepository.deleteByTestRunId(it.testRunId) }
		toDelete.forEach { testLogsRepository.deleteByTestRunId(it.testRunId) }
	}

	@DeleteMapping("/statistics/{starttime}/{endtime}")
	fun deleteAllInTimerange(@PathVariable starttime: String, @PathVariable endtime: String,
							 @ApiParam("A boolean check to validate if the deletion is confirmed")
							 @RequestParam("confirmed", defaultValue = "false") confirmed: Boolean) {
		//Parse the dates (optionally with time)
		val start = try {
			LocalDate.parse(starttime).atStartOfDay()
		} catch (e: DateTimeParseException) {
			LocalDateTime.parse(starttime).minusMinutes(1)
		}
		val end = try {
			LocalDate.parse(endtime).plusDays(1).atStartOfDay() //This is the same as the end of the day
		} catch (e: DateTimeParseException) {
			LocalDateTime.parse(endtime).plusMinutes(1)
		}

		//Find the data to be deleted
		val testruns = testRunRepository.findAllByTestCases_Steps_TimestampBetween(start, end).toList()
		val testRunIds = testruns.map { it.testRunId }.distinct()
		val testlogs = testRunIds.map { testLogsRepository.findAllByTestRunId(it) }.flatten()
		val testmodels = testRunIds.map { testModelRepository.findAllByTestRunId(it) }.flatten()

		println("Deleting ${testruns.size + testlogs.size + testmodels.size} entries from the database")
		if (confirmed) {
			//Delete everything from the database
			testRunRepository.deleteAll(testruns)
			testLogsRepository.deleteAll(testlogs)
			testModelRepository.deleteAll(testmodels)
		} else {
			throw IllegalArgumentException("Please confirm you request to delete the following items: $testruns, $testlogs, $testmodels")
		}
	}

	@GetMapping("/statistics/sut")
	fun showAllBySutFilename(@RequestHeader("filename") filename: String): ResponseEntity<Iterable<TestLogs>> {
		val res = testLogsRepository.findAllBySutFilename(filename)
		return if (res.count() > 0) {
			ResponseEntity.ok(res)
		} else {
			ResponseEntity.noContent().build()
		}
	}
}