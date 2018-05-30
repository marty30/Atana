package nl.utwente.axini.atana.controllers

import com.fasterxml.jackson.core.JsonFactory
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import nl.utwente.axini.atana.models.TestLogs
import nl.utwente.axini.atana.models.TestResult
import nl.utwente.axini.atana.models.TestRun
import nl.utwente.axini.atana.models.jsonObjectMapper
import nl.utwente.axini.atana.repository.TestLogsRepository
import nl.utwente.axini.atana.repository.TestModelRepository
import nl.utwente.axini.atana.repository.TestRunRepository
import nl.utwente.axini.atana.transaction
import org.hibernate.Session
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.persistence.EntityManager
import javax.servlet.http.HttpServletResponse

data class Statistics(val testRunId: UUID?, val verdict: TestResult, val totalCount: Long?, val passedCount: Long?, val failedCount: Long?) {
	operator fun plus(other: Statistics): Statistics {
		if (other.testRunId != testRunId){
			throw IllegalArgumentException("Test run ids do not match")
		}
		return Statistics(testRunId, verdict, (totalCount?:0)+(other.totalCount?:0), (passedCount?:0)+(other.passedCount?:0), (failedCount?:0)+(other.failedCount?:0))
	}
}

@RestController
class StatisticsController(val entityManager: EntityManager, val testRunRepository: TestRunRepository, val testLogsRepository: TestLogsRepository, val testModelRepository: TestModelRepository) : AbstractController() {

	fun getBaseStatistics(): Stream<MutableMap<String, out Any?>> {
		val session = entityManager.unwrap(Session::class.java)
//		val q_insert = session.createNativeQuery("CREATE VIEW tmp_view AS SELECT tr.test_run_id, tc.verdict, sum(tc.verdict=1) as verdict1, sum(tc.verdict=0) as verdict0 FROM `test_run` as tr, `test_case` as tc, `test_run_test_cases` as tr_tc WHERE tr.database_id = tr_tc.test_run_database_id AND tc.database_id = tr_tc.test_cases_database_id GROUP BY tr.test_run_id, tc.verdict")
//		val q = session.createQuery("SELECT tr.testRunId, tc.verdict, sum(case when tc.verdict=1 then 1 else 0 end) as verdict1, sum(case when tc.verdict=0 then 1 else 0 end) as verdict0, (verdict1/verdict0) FROM TestRun as tr join tr.testCases as tc GROUP BY tr.testRunId, tc.verdict")
		val q = session.createQuery("SELECT new list(tr.testRunId, tc.verdict, count(tc), sum(case when tc.verdict=:passed then 1 else 0 end), sum(case when tc.verdict=:failed then 1 else 0 end)) FROM TestRun as tr join tr.testCases as tc GROUP BY tr.testRunId, tc.verdict").setParameter("passed", TestResult.PASSED).setParameter("failed", TestResult.FAILED)
//		transaction{
//			q_insert.executeUpdate()
//		}
//		val q = session.createQuery("SELECT l.testRunId, sum(l.verdict1), sum(l.verdict0) FROM tmp_view as l GROUP BY l.testRunId")
		val res = q.stream().map {it ->
			val i = it as List<Any?>
			Statistics(it[0] as UUID, it[1] as TestResult, it[2] as Long, it[3] as Long, it[4] as Long)
		}.flatMap(object : Function<Statistics, Stream<Statistics>> {
			var prev: Statistics? = null
			override fun apply(it: Statistics): Stream<Statistics> {
				val res = if (prev?.testRunId == it.testRunId) {
					Stream.of(it + prev!!)
				} else
					Stream.empty<Statistics>()
				prev = it
				return res
			}
		})
//		log.warn("3")
//		val crit = entityManager.criteriaBuilder.createQuery(TestRun::class.java)
//		val tr = crit.from(TestRun::class.java)
//		val tc = crit.from(TestCase::class.java)
//		crit.select(tr)

//		log.warn("4")
//		val result = entityManager.createQuery(crit).resultStream
//		log.warn("5")
		return res.map {
//			log.warn("6")
			val map	= mutableMapOf(
					"test_run_id" to it.testRunId,
					"total_count" to it.totalCount,
					"passed_count" to it.passedCount,
					"failed_count" to it.failedCount,
					"passed_percentage" to ((it.passedCount?:0).toFloat() / (it.totalCount?:0).toFloat()),
					"failed_percentage" to ((it.failedCount?:0).toFloat() / (it.totalCount?:0).toFloat())
			)
//			log.warn("7")
			val logs = testLogsRepository.findAllByTestRunId(it.testRunId!!).toList()
//			log.warn("8")
			if (logs.isNotEmpty()) {
				if (logs.size > 1) {
					log.warn("Multiple keys found: $logs")
				}
				map["test_set_id"] = logs[0].testsetId
				map["sut_filename"] = (logs[0].sutFilename ?: "")
			}
			log.warn("9")
			return@map map
		}
	}

	private fun writeToResponse(stream: Stream<out Any>, response : HttpServletResponse) {
//		log.warn("10")
		val jsonFactory = JsonFactory()
		val gen = jsonFactory.createGenerator(response.outputStream)
		gen.codec = jsonObjectMapper.value
		response.contentType = MediaType.APPLICATION_JSON_UTF8_VALUE
//		log.warn("11")

		gen.writeStartArray()
//		log.warn("12")
		stream.forEach{
			gen.writeObject(it)
			response.flushBuffer()
//			log.warn("13")
		}
		gen.writeEndArray()
		gen.close()
//		log.warn("14")
	}

	@GetMapping("/statistics")
	@ApiOperation("Show all statistics")
	fun showStatistics(response : HttpServletResponse) {
//		log.warn("1")
		writeToResponse(getBaseStatistics(), response)
//		log.warn("2")
	}

	@GetMapping("/statistics/usable")
	@ApiOperation("Show tests that are usable")
	fun showUsableTestRuns(response : HttpServletResponse) {
		val minPercentage = 0.05
		val maxPercentage = 0.95

		writeToResponse(getBaseStatistics().filter {
			it["failed_percentage"] as Float in minPercentage..maxPercentage &&
					it["passed_percentage"] as Float in minPercentage..maxPercentage
		}, response)
	}

	@GetMapping("/statistics/unusable")
	@ApiOperation("Show tests that are NOT usable")
	fun showUnusableTestRuns(response : HttpServletResponse) {
		val minPercentage = 0.05
		val maxPercentage = 0.95

		writeToResponse(getBaseStatistics().filter {
			it["failed_percentage"] as Float !in minPercentage..maxPercentage &&
					it["passed_percentage"] as Float !in minPercentage..maxPercentage
		}, response)
	}

	@DeleteMapping("/statistics/unusable")
	@ApiOperation("Delete tests that are NOT usable")
	fun deleteUnusableTestRuns() {
		val minPercentage = 0.05
		val maxPercentage = 0.95

		getBaseStatistics().filter {
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