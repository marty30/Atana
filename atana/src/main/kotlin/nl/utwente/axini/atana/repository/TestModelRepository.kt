package nl.utwente.axini.atana.repository

import nl.utwente.axini.atana.models.TestModel
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface TestModelRepository : CrudRepository<TestModel, Long> {
	@Transactional
	fun deleteByTestRunId(testRunId: UUID)

	fun findAllByTestRunId(testRunId: UUID): Iterable<TestModel>
}

fun findAllTestRunIdsWithSimilarModels(repository: TestModelRepository): List<Pair<TestModel, List<UUID>>> {
	return repository.findAll().map { Pair(it.testRunId, it.copy(testRunId = null)) }.groupBy { it.second }.map { Pair(it.key, it.value.mapNotNull { it.first })}
}