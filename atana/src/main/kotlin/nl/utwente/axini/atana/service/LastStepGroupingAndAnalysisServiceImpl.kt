package nl.utwente.axini.atana.service

import nl.utwente.axini.atana.models.AnalysisResult
import nl.utwente.axini.atana.models.TestCase
import nl.utwente.axini.atana.models.TestModel
import org.springframework.stereotype.Service

@Service
class LastStepGroupingAndAnalysisServiceImpl : GroupingAndAnalysisService() {
	override fun isConfigured(): Boolean {
		return true
	}

	override fun submitAllData(model: TestModel, passingTests: Array<TestCase>, failingTests: Array<TestCase>, coverageInformation: List<TestModel>) {
		//Do nothing
	}

	override fun submitTest(run: TestCase): AnalysisResult {
		val problematicStep = run.steps.sortedBy { it.stepNumber.toInt() }.last()
		return AnalysisResult(null, run.caseindex, "Transition ${problematicStep.fullLabel}", null, null, setOf(problematicStep.copy(labelParameters = HashMap(problematicStep.labelParameters), notes = HashSet(problematicStep.notes))))
	}

}