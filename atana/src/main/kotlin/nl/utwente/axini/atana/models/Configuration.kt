package nl.utwente.axini.atana.models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.annotations.ApiModelProperty
import nl.utwente.axini.atana.serialization.KClassDeserializer
import nl.utwente.axini.atana.serialization.KClassSerializer
import nl.utwente.axini.atana.service.GroupingAndAnalysisService
import nl.utwente.axini.atana.validation.SubclassOf
import java.net.URL
import kotlin.reflect.KClass

data class Configuration(
        @ApiModelProperty(notes = "Endpoint for the implementation of the grouping and analysis service", required = false, example = "http://localhost:8090/rest")
        val endpoint: URL? = null,

        @ApiModelProperty(notes = "Class of the implementation of the grouping and analysis service", required = true, example = "nl.utwente.axini.atana.service.GroupingAndAnalysisServiceDummyImpl")
        @get:SubclassOf(GroupingAndAnalysisService::class)
        @JsonSerialize(using = KClassSerializer::class)
        @JsonDeserialize(using = KClassDeserializer::class)
        val groupingAndAnalysisServiceImplementation: KClass<out Any>,

        @ApiModelProperty(notes = "A set of key-value pairs as config for the grouping and analysis service", required = false, example = "{\"key\": \"value\"}")
        val groupingAndAnalysisServiceConfiguration: Map<String, String>? = mapOf()
)