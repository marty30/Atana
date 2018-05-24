package nl.utwente.axini.atana.repository

import nl.utwente.axini.atana.models.TestLogs
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface TestLogsRepository : CrudRepository<TestLogs, Long> {
	@Transactional
	fun deleteByTestRunId(testRunId: UUID)

	fun findAllByTestRunId(testRunId: UUID): Iterable<TestLogs>

	fun findAllBySutFilename(filename: String): Iterable<TestLogs>
}