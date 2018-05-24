package nl.utwente.axini.atana.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer
import com.fasterxml.jackson.databind.ser.std.UUIDSerializer
import io.swagger.annotations.ApiModelProperty
import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty

@Entity
data class AnalysisResult(
		@ApiModelProperty(example = "3ec4d0e1-2431-4516-ab3e-f15b061603e3")
		@JsonProperty("test_run_id")
		@JsonSerialize(using = UUIDSerializer::class)
		@JsonDeserialize(using = UUIDDeserializer::class)
		@Type(type = "uuid-char")
		var testRunId: UUID?,

		@get:Min(0)
		var testCaseIndex: Int?,

		@get:NotEmpty
		@Type(type = "text")
		val groupName: String,

		@OneToOne(cascade = [CascadeType.ALL])
		@get:Valid
		val rootCauseState: State?,

		@OneToOne(cascade = [CascadeType.ALL])
		@get:Valid
		val rootCauseTransition: Transition?,

		/**
		 * This field MUST contain a set, but it can be an empty set if there is no problematic step found
		 */
		@OneToMany(cascade = [CascadeType.ALL])
		@get:Valid
		val rootCauseSteps: Set<Step>
) : AbstractModel()