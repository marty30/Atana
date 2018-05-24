package nl.utwente.axini.atana.validation

import nl.utwente.axini.atana.controllers.InvalidRequestDataException
import nl.utwente.axini.atana.models.*
import nl.utwente.axini.atana.service.GroupingAndAnalysisServiceDummyImpl
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.net.URL
import java.time.LocalDateTime
import java.util.*

class ValidationUtilsKtTest {

	@Test
	fun validateValidObject() {
		validate(AnalysisResult(UUID.randomUUID(), 1, "State and transition fault", null, null, setOf()))
		validate(Configuration(URL("http://localhost"), GroupingAndAnalysisServiceDummyImpl::class))
		validate(TestLogs(UUID.randomUUID(), null, UUID.randomUUID(), "first log", "second log", "third log"))
		validate(TestModel(UUID.randomUUID(), null, setOf(Sts("Some name", setOf(State("some id", StateAttribute("Some label", "Some type", null))), setOf(ChildModel("Some id", ChildModelAttribute("Some label", "Some type", "Some id", null))), setOf(StartState("Some id", null)), setOf(Transition("Some id", "Some id", TransitionAttribute("Some label", "Some type", null))), setOf(Transition("Some id", "Some id", TransitionAttribute("Some label", "Some type", null))), "some id", null, null)), null))
		validate(TestRun(UUID.randomUUID(), setOf(TestCase(-1, null, TestResult.PASSED, null, setOf(Step(Label("Some label", "right", "regular"), null, LocalDateTime.now(), setOf(), 1, null, null, "Some label")), 1, setOf(ExpectedLabel(Label("Some label", "right", "regular"), LocalDateTime.now())), setOf()))))
	}

	//TODO validate the error messages produced by "validate"
	@Test
	fun validateInvalidObject() {
		assertAll(
				{ assertThrows<InvalidRequestDataException>("AnalysisResult was unexpectedly valid") { validate(AnalysisResult(UUID.randomUUID(), -10, "", null, null, setOf())) } },
				{ assertThrows<InvalidRequestDataException>("Configuration was unexpectedly valid") { validate(Configuration(URL("http://localhost"), Any::class)) } },
				{ assertThrows<InvalidRequestDataException>("TestModel was unexpectedly valid") { validate(TestModel(UUID.randomUUID(), null, setOf(Sts("", setOf(), setOf(), setOf(), setOf(), setOf(), "", null, null)), null)) } },
				{ assertThrows<InvalidRequestDataException>("TestRun was unexpectedly valid") { validate(TestRun(UUID.randomUUID(), setOf(TestCase(-10, null, TestResult.PASSED, null, setOf(Step(Label("", "", ""), null, LocalDateTime.now().plusDays(1), setOf(), -1)), 1, setOf(), setOf())))) } }
		)
	}

	@Test
	fun validateData() {
		val ex = assertThrows<InvalidRequestDataException> { validateData(false) { "some error message" } }
		assertEquals(ex.message, "some error message")
		validateData(true) { "This should not be thrown" }
	}
}