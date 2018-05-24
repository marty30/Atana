package nl.utwente.axini.atana

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.persistence.AttributeConverter
import javax.persistence.Converter
import kotlin.reflect.full.companionObject


@SpringBootApplication
class AtanaApplication

@Configuration
class Config {
	@Bean
	fun restTemplate() = RestTemplate()
}

@Converter(autoApply = true)
class LocalDateTimeAttributeConverter : AttributeConverter<LocalDateTime, Timestamp> {

	override fun convertToDatabaseColumn(locDateTime: LocalDateTime?): Timestamp? {
		return if (locDateTime == null) null else Timestamp.valueOf(locDateTime)
	}

	override fun convertToEntityAttribute(sqlTimestamp: Timestamp?): LocalDateTime? {
		return sqlTimestamp?.toLocalDateTime()
	}
}

fun jsonObjectMapper(): ObjectMapper {
	return jacksonObjectMapper()
			.registerModule(JavaTimeModule())
			.registerModule(Jdk8Module())
			.registerModule(ParameterNamesModule())
			.findAndRegisterModules()
}

val mavenModel = lazy {
	try {
		return@lazy MavenXpp3Reader().read(FileReader(File("pom.xml")))
	} catch (e: FileNotFoundException) {
		if (File("/META-INF/maven/nl.utwente.axini/atana/pom.xml").exists()) {
			return@lazy MavenXpp3Reader().read(FileReader(File("/META-INF/maven/nl.utwente.axini/atana/pom.xml")))
		} else {
			val m = Model()
			m.version = when {
				!AtanaApplication::class.java.`package`.implementationVersion.isNullOrBlank() -> AtanaApplication::class.java.`package`.implementationVersion
				else -> {
					e.printStackTrace()
					"Unknown"
				}
			}
			return@lazy m
		}
	}
}

fun main(args: Array<String>) {
	runApplication<AtanaApplication>(*args)
}


fun <R : Any> R.logger(): Lazy<Logger> {
	return lazy { LogManager.getLogger(unwrapCompanionClass(this.javaClass).name) }
}

// unwrap companion class to enclosing class given a Java Class
private fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
	return if (ofClass.enclosingClass != null && ofClass.enclosingClass.kotlin.companionObject?.java == ofClass) {
		ofClass.enclosingClass
	} else {
		ofClass
	}
}
