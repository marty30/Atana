package nl.utwente.axini.atana

import com.fasterxml.jackson.module.kotlin.readValue
import nl.utwente.axini.atana.models.TestModel
import nl.utwente.axini.atana.models.jsonObjectMapper
import java.io.File

object TestModelFactory {
	val sampleModel: TestModel
		get() = jsonObjectMapper.value.readValue(File(this.javaClass.classLoader.getResource("testmodel.json").file).readText())
}