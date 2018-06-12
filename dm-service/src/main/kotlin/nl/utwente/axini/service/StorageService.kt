package nl.utwente.axini.service

import nl.utwente.axini.atana.models.TestCase
import nl.utwente.axini.atana.models.TestModel
import nl.utwente.axini.model.Config
import nl.utwente.axini.model.ConversionMethod
import nl.utwente.axini.utils.InvalidatableLazyImpl
import org.springframework.stereotype.Service
import weka.core.Attribute
import weka.core.DenseInstance
import weka.core.Instance
import weka.core.Instances

@Service
class StorageService {
	final val specialNumericAttributes = listOf("nr_of_steps", "nr_of_reoccurring_steps")
	final val specialStringAttributes = listOf("first_step", "last_step")

	var config: Config? = null

	private val passingTests: MutableList<TestCase> = mutableListOf()
	private val failingTests: MutableList<TestCase> = mutableListOf()
	var model: TestModel? = null

	lateinit var instancesHeaders: ArrayList<Attribute>
	private final val instancesDelegate = InvalidatableLazyImpl({
		//NOTE in this way of structuring the data, the order of the steps does not matter, but the set is smaller and has no missing data
		val data = passingTests.asSequence() + failingTests.asSequence()
		val headerAttributes = data.map { it.steps }.flatten().map { Attribute(it.fullLabel) }.distinct().toMutableList() as ArrayList
		headerAttributes.addAll(specialNumericAttributes.map { Attribute(it) })
//		headerAttributes.addAll(specialStringAttributes.map { Attribute(it, true) })
		instancesHeaders = headerAttributes

		val dataInstances = Instances("testcases_steps", headerAttributes, data.count())

		data.map { it.steps }.forEach { steps ->
			val instance = DenseInstance(headerAttributes.size)
			headerAttributes.forEach { attribute ->
				if (attribute.name() in specialStringAttributes || attribute.name() in specialNumericAttributes) {
					when (attribute.name()) {
						"nr_of_steps" -> instance.setValue(attribute, steps.size.toDouble())
						"nr_of_reoccurring_steps" -> instance.setValue(attribute, steps.groupBy { it.fullLabel }.filter { it.value.size > 1 }.count().toDouble())
						"first_step" -> instance.setValue(attribute, steps.first().fullLabel)
						"last_step" -> instance.setValue(attribute, steps.last().fullLabel)
					}
				} else {
					when (config?.conversionMethod) {
						ConversionMethod.NEGATIVE_COUNT -> {
							val stepCount = steps.map { it.fullLabel }.filter { it == attribute.name() }.count()
							instance.setValue(attribute, stepCount.toDouble() * -1)
						}
						ConversionMethod.PRESENCE -> {
							instance.setValue(attribute, if (steps.map { it.fullLabel }.contains(attribute.name())) 1.0 else 0.0)
						}
						ConversionMethod.INDEX -> {
							instance.setValue(attribute, steps.map { it.fullLabel }.indexOf(attribute.name()).toDouble())
						}
					}
				}
			}
			dataInstances.add(instance)
		}
		return@InvalidatableLazyImpl dataInstances
	})
	val instances: Instances by instancesDelegate
	lateinit var classifiedInstances: Map<Instance, Int>

	fun addFailingTest(test: TestCase) {
		instancesDelegate.invalidate()
		failingTests.add(test)
	}

	fun addPassingTest(test: TestCase) {
		instancesDelegate.invalidate()
		passingTests.add(test)
	}

	fun addAllPassingTests(testCases: Collection<TestCase>, override: Boolean = true) {
		instancesDelegate.invalidate()
		if (override) {
			passingTests.removeAll { true }
		}
		passingTests.addAll(testCases)
	}

	fun addAllFailingTests(testCases: Collection<TestCase>, override: Boolean = true) {
		instancesDelegate.invalidate()
		if (override) {
			passingTests.removeAll { true }
		}
		passingTests.addAll(testCases)
	}
}