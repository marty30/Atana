package nl.utwente.axini.model

import nl.utwente.axini.utils.InvalidatableLazyImpl
import weka.classifiers.AbstractClassifier
import weka.clusterers.AbstractClusterer
import weka.core.Capabilities
import weka.core.Instances
import weka.core.Utils
import weka.core.converters.ConverterUtils
import java.io.ByteArrayInputStream
import java.util.*

data class Config(
		val clusterer: String,
		val clustererOptions: String,
		val conversionMethod: ConversionMethod = ConversionMethod.NEGATIVE_COUNT,
		val RCAClassifier: String?,
		val RCAClassifierOptions: String?,
		private val RCATrainingFile: String?
) {
	//Validation of the data//
	fun isValid(): Boolean {
		return isClustererValid()
				&& isClustererOptionsValid()
				&& clustererHasCapabilities()
	}

	private fun isClustererValid(): Boolean {
		return try {
			val clazz = Class.forName(clusterer)
			AbstractClusterer::class.java.isAssignableFrom(clazz) && clazz.newInstance() != null
		} catch (e: ClassNotFoundException) {
			false
		} catch (e2: InstantiationException) {
			false
		}
	}

	private fun isClustererOptionsValid(): Boolean {
		return try {
			weka.core.Utils.splitOptions(clustererOptions)
			true
		} catch (e: Exception) {
			e.printStackTrace()
			false
		}
	}

	private fun clustererHasCapabilities(): Boolean {
		val c = Utils.forName(AbstractClusterer::class.java, clusterer, weka.core.Utils.splitOptions(clustererOptions)) as AbstractClusterer
		return c.capabilities.handles(Capabilities.Capability.NO_CLASS)
	}

	//Clusterer stuff//
	final val classifierDelegate = InvalidatableLazyImpl({
		createClusterer()
	})
	val classifierModel: AbstractClusterer by classifierDelegate

	fun createClusterer(): AbstractClusterer {
		return Utils.forName(AbstractClusterer::class.java, clusterer, weka.core.Utils.splitOptions(clustererOptions)) as AbstractClusterer
	}

	//Classifier stuff//
	final val RCAClassifierDelegate = InvalidatableLazyImpl({
		createRCAClassifier()
	})
	fun unwrapRCATrainingFile() : Instances? {
		if (RCATrainingFile.isNullOrBlank()) return null
		val lines = String(Base64.getDecoder().decode(RCATrainingFile))
		val i = ConverterUtils.DataSource(ByteArrayInputStream(lines.toByteArray())).dataSet
		i.setClassIndex(i.numAttributes()-1)
		return i
	}

	fun createRCAClassifier(): AbstractClassifier? {
		if (RCAClassifier.isNullOrBlank()) return null
		return Utils.forName(AbstractClassifier::class.java, RCAClassifier, weka.core.Utils.splitOptions(RCAClassifierOptions)) as AbstractClassifier
	}
	val RCAClassifierModel: AbstractClassifier? by RCAClassifierDelegate
}

/**
 * This determines how the json test cases have to be converted into Weka Instances.
 */
enum class ConversionMethod{
	/**
	 * NEGATIVE_COUNT is the method where each attribute contains the number of occurrences of that particular step multiplied by -1.
	 */
	NEGATIVE_COUNT,
	/**
	 * PRESENCE is the method where each attribute contains a 1.0 if the step occurs and 0.0 otherwise
	 */
	PRESENCE,
	/**
	 * INDEX is the method where each attribute contains the index of the first occurence of that particular step
	 */
	INDEX
}