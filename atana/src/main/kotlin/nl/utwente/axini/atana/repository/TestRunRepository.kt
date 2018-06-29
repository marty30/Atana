package nl.utwente.axini.atana.repository

import nl.utwente.axini.atana.models.TestResult
import nl.utwente.axini.atana.models.TestRun
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Repository
interface TestRunRepository : CrudRepository<TestRun, Long> {
    @Transactional
    fun deleteByTestRunId(testRunId: UUID)

    fun findAllByTestRunId(testRunId: UUID): Iterable<TestRun>

    fun findAllByTestCases_Verdict(verdict: TestResult): Iterable<TestRun>

    fun findAllByTestCases_Steps_TimestampBetween(starttime: LocalDateTime, endtime: LocalDateTime): Iterable<TestRun>
}