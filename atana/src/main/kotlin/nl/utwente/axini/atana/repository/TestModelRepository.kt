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