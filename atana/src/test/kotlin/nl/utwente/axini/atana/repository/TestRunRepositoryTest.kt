package nl.utwente.axini.atana.repository

import nl.utwente.axini.atana.AtanaApplication
import nl.utwente.axini.atana.models.*
import nl.utwente.axini.atana.transaction
import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.MatcherAssert.assertThat
import org.hibernate.Hibernate
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [AtanaApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestRunRepositoryTest {
	@Autowired
	lateinit var repository: TestRunRepository

	@Test
	fun findAllByTestCasesVerdictTest() {
		//Given
		val step1 = Step(Label("step 1", "0", "1"), null, LocalDateTime.now(), null, 1)
		val step2 = Step(Label("step 2", "1", "2"), null, LocalDateTime.now(), null, 2)
		val step3 = Step(Label("step 3", "2", "3"), null, LocalDateTime.now(), null, 3)

		//Fully passing run
		val fullyPassing = TestRun(UUID.randomUUID(), setOf(
				TestCase(-1, null, TestResult.PASSED, null, setOf(step1.copy(), step2.copy(), step3.copy()), 3),
				TestCase(-1, null, TestResult.PASSED, null, setOf(step1.copy(), step2.copy(), step3.copy()), 3),
				TestCase(-1, null, TestResult.PASSED, null, setOf(step1.copy(), step2.copy(), step3.copy()), 3)
		))
		//Partially passing run
		val partialPassing = TestRun(UUID.randomUUID(), setOf(
				TestCase(-1, null, TestResult.PASSED, null, setOf(step1.copy(), step2.copy(), step3.copy()), 3),
				TestCase(-1, null, TestResult.PASSED, null, setOf(step1.copy(), step2.copy(), step3.copy()), 3),
				TestCase(-1, null, TestResult.FAILED, null, setOf(step1.copy(), step2.copy(), step3.copy()), 3)
		))
		//Fully failing run
		val fullyFailed = TestRun(UUID.randomUUID(), setOf(
				TestCase(-1, null, TestResult.FAILED, null, setOf(step1.copy(), step2.copy(), step3.copy()), 3),
				TestCase(-1, null, TestResult.FAILED, null, setOf(step1.copy(), step2.copy(), step3.copy()), 3),
				TestCase(-1, null, TestResult.FAILED, null, setOf(step1.copy(), step2.copy(), step3.copy()), 3)
		))
		repository.saveAll(listOf(fullyPassing, partialPassing, fullyFailed))

		//When
		val (foundPassed, foundFailed) = transaction <Pair<Iterable<TestRun>, Iterable<TestRun>>> {
			val foundPassed = repository.findAllByTestCases_Verdict(TestResult.PASSED)
			val foundFailed = repository.findAllByTestCases_Verdict(TestResult.FAILED)
			foundPassed.forEach({ Hibernate.initialize(it.testCases) })
			foundFailed.forEach({ Hibernate.initialize(it.testCases) })
			return@transaction Pair(foundPassed, foundFailed)
		}

		//Then
		assertThat(foundPassed.map { it.testRunId }, hasItems(fullyPassing.testRunId, partialPassing.testRunId))
		assertThat(foundFailed.map { it.testRunId }, hasItems(fullyFailed.testRunId, partialPassing.testRunId))
	}
}