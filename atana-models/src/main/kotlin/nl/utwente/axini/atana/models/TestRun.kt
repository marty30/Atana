package nl.utwente.axini.atana.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer
import com.fasterxml.jackson.databind.ser.std.UUIDSerializer
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import nl.utwente.axini.atana.serialization.ZonedDateTimeDeserializer
import nl.utwente.axini.atana.serialization.ZonedDateTimeSerializer
import org.hibernate.annotations.Type
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty

@ApiModel
@Entity
data class TestRun(
		@ApiModelProperty(example = "3ec4d0e1-2431-4516-ab3e-f15b061603e3")
		@JsonProperty("test_run_id")
		@JsonSerialize(using = UUIDSerializer::class)
		@JsonDeserialize(using = UUIDDeserializer::class)
		@Type(type = "uuid-char")
		val testRunId: UUID,

		@OneToMany(cascade = [CascadeType.ALL])
		@JsonProperty("test_cases")
		@get:Valid
		@get:NotEmpty
		val testCases: Set<TestCase>
) : AbstractModel()

@ApiModel
@Entity
data class TestCase(
		@get:Min(-1)
		@JsonProperty("index")
		val caseindex: Int? = -1,

		@get:Min(-1)
		val id: Int? = -1,

		val verdict: TestResult,

		@JsonProperty("error_message")
		@Type(type = "text")
		val errorMessage: String? = "",

		@OneToMany(cascade = [CascadeType.ALL])
		@get:Valid
		@get:NotEmpty
		val steps: Set<Step>,

		@ApiModelProperty(example = "1")
		@get:Min(0)
		val last_step: Int,

		@OneToMany(cascade = [CascadeType.ALL])
		@get:Valid
		val expected_labels: Set<ExpectedLabel>? = setOf(),

		@ElementCollection
		val tags: Set<String>? = setOf()
) : AbstractModel()

@ApiModel
@Entity
data class Step(
		@OneToOne(cascade = [CascadeType.ALL])
		@get:Valid
		val label: Label,

		private var _fullLabel: String? = null,

		@JsonSerialize(using = ZonedDateTimeSerializer::class)
		@JsonDeserialize(using = ZonedDateTimeDeserializer::class)
		val timestamp: LocalDateTime,

		@ElementCollection
		val notes: Set<String>? = setOf(),

		@Column(name = "step_number")
		@ApiModelProperty(example = "1")
		@get:Min(0)
		@JsonProperty("step_number")
		val stepNumber: Int,

		@JsonProperty("state_vector_size")
		val stateVectorSize: String? = "",

		@JsonProperty("advance_duration_ms")
		val advanceDuration: String? = "",

		@JsonProperty("physical_label")
		val physicalLabel: String? = "",

		@JsonProperty("label_parameters")
		@ElementCollection
		val labelParameters: Map<String, String>? = mapOf()
) : AbstractModel() {
	var fullLabel = _fullLabel
		get(): String? {
			if (_fullLabel == null) {
				_fullLabel = ""
				if (label.direction == "in" || label.direction == "stimulus") {
					_fullLabel += ('?')
				} else if (label.direction == "out" || label.direction == "response") {
					_fullLabel += ('!')
				}
				_fullLabel += (label.name)

				if (labelParameters != null) {
					if (!labelParameters.isEmpty()) {
						_fullLabel += (" if (")
						val labelParamArgs = mutableListOf<String>()
						for (label_param in labelParameters.entries) {
							labelParamArgs.add("(${label_param.key} == \\\"${label_param.value}\\\")")
						}
						_fullLabel += (labelParamArgs.joinToString(" && "))
						_fullLabel += (")")
					}
				}
			}
			return _fullLabel
		}
}

@ApiModel
@Entity
data class Label(
		@get:NotEmpty
		val name: String,

		@get:NotEmpty
		val direction: String,

		val channel: String?
) : AbstractModel()

@ApiModel
@Entity
data class ExpectedLabel(
		@OneToOne(cascade = [CascadeType.ALL])
		@get:Valid
		val label: Label,

		@JsonSerialize(using = ZonedDateTimeSerializer::class)
		@JsonDeserialize(using = ZonedDateTimeDeserializer::class)
		val deadline: LocalDateTime?

//		@ElementCollection
//		@JsonProperty("constraint")
//		val constraints: Map<String, Any>? = mapOf() //TODO Any is not a valid option
) : AbstractModel()

enum class TestResult {
	PASSED, FAILED, ERROR, UNKNOWN;

	@JsonValue
	override fun toString(): String {
		return this.name.toLowerCase()
	}
}
