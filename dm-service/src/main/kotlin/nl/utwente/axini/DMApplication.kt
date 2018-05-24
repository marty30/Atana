package nl.utwente.axini

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DMApplication


fun main(args: Array<String>) {
	runApplication<DMApplication>(*args)
}

fun jsonObjectMapper(): ObjectMapper {
	return jacksonObjectMapper()
			.registerModule(JavaTimeModule())
			.registerModule(Jdk8Module())
			.registerModule(ParameterNamesModule())
			.findAndRegisterModules()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

//TODO Kanstrén et al also used the reduced set of traces for pattern mining. First they summarised the number of different steps required to reach the failure. Secondly they ordered the trace sequences.