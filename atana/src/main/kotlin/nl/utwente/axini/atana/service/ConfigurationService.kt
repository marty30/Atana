package nl.utwente.axini.atana.service

import nl.utwente.axini.atana.logger
import nl.utwente.axini.atana.models.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileReader
import java.net.URL
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Service
class ConfigurationService {
	val log by logger()

	var config = Configuration(endpoint = URL("http://localhost:8000/data"), groupingAndAnalysisServiceImplementation = GroupingAndAnalysisServiceRestImpl::class)
	/**
	 * This varible shows if the config is "fresh". A config is "fresh" is has not yet been used by any grouping or analysis service during configuration. When the service is configured, the config is not fresh anymore until an new config is uploaded.
	 */
	val freshConfig = AtomicBoolean(true)

	@Autowired
	lateinit var environment: ConfigurableEnvironment

	companion object {
		@JvmField
		val REQUIRED_KEYS = listOf("groupingAndAnalysisServiceImplementation")
	}

	fun loadFromFile(file: File) {
		require(file.exists()) { "The file $file should exist, but it does not." }
		val properties = Properties()
		properties.load(FileReader(file))
		loadFromProperties(properties)
		log.info("Loaded properties from %s".format(file.absolutePath))
	}

	fun loadFromProperties() {
		val p = Properties()
		val key = "atana.config."
		environment.propertySources
				.filterIsInstance<EnumerablePropertySource<*>>()
				.forEach {
					it.propertyNames
							.filter { it.startsWith(key) && it.length > key.length }
							.forEach { k -> p[k.substring(key.length)] = it.getProperty(k) }
				}

		loadFromProperties(p)
	}

	fun loadFromProperties(properties: Properties) {
		//Note, also include security since this is tainted data
		require(!properties.isEmpty) { "The given properties may not be empty" }
		require(REQUIRED_KEYS.all { properties.containsKey(it) }) { "The following properties are all required: $REQUIRED_KEYS" }
		config = Configuration(URL(properties.getProperty("endpoint")), Class.forName(properties.getProperty("groupingAndAnalysisServiceImplementation")).kotlin)
	}

}
