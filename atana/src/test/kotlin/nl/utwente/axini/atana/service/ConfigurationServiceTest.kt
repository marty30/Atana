package nl.utwente.axini.atana.service

import com.nhaarman.mockito_kotlin.spy
import nl.utwente.axini.atana.models.Configuration
import org.junit.Assert.assertEquals
import org.junit.Test
import org.springframework.mock.env.MockEnvironment
import java.io.File
import java.net.URL
import java.util.*

class ConfigurationServiceTest {
	private val configurationService = spy<ConfigurationService>()

	@Test
	fun loadFromFile() {
		//Given
		val tempProperties = File.createTempFile("temp", ".properties")
		tempProperties.writeText(
				"endpoint=http://localhost:4000\n" +
						"groupingAndAnalysisServiceImplementation=nl.utwente.axini.atana.service.GroupingAndAnalysisServiceDummyImpl\n"
		)

		//When
		configurationService.loadFromFile(tempProperties)

		//Then
		assertEquals(configurationService.config, Configuration(URL("http://localhost:4000"), GroupingAndAnalysisServiceDummyImpl::class))
	}

	@Test
	fun loadFromSystemProperties() {
		//Given
		val environment = MockEnvironment()
		environment.withProperty("atana.config.endpoint", "http://localhost:3000")
		environment.withProperty("atana.config.groupingAndAnalysisServiceImplementation", "nl.utwente.axini.atana.service.GroupingAndAnalysisServiceDummyImpl")
		configurationService.environment = environment

		//When
		configurationService.loadFromProperties()

		//Then
		assertEquals(configurationService.config, Configuration(URL("http://localhost:3000"), GroupingAndAnalysisServiceDummyImpl::class))
	}

	@Test
	fun loadFromProperties() {
		//Given
		val p = Properties()
		p.setProperty("endpoint", "http://localhost:2000")
		p.setProperty("groupingAndAnalysisServiceImplementation", "nl.utwente.axini.atana.service.GroupingAndAnalysisServiceDummyImpl")

		//When
		configurationService.loadFromProperties(p)

		//Then
		assertEquals(configurationService.config, Configuration(URL("http://localhost:2000"), GroupingAndAnalysisServiceDummyImpl::class))
	}
}