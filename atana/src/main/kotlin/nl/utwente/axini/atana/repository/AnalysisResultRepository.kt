package nl.utwente.axini.atana.repository

import nl.utwente.axini.atana.models.AnalysisResult
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AnalysisResultRepository : CrudRepository<AnalysisResult, Long> {
    fun findAllByTestRunId(testRunId: UUID?): Iterable<AnalysisResult>
}