package nl.utwente.axini.atana.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer
import com.fasterxml.jackson.databind.ser.std.UUIDSerializer
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity

@ApiModel
@Entity
data class TestLogs(
		@ApiModelProperty(example = "3ec4d0e1-2431-4516-ab3e-f15b061603e3")
		@JsonProperty("test_run_id")
		@JsonSerialize(using = UUIDSerializer::class)
		@JsonDeserialize(using = UUIDDeserializer::class)
		@Type(type = "uuid-char")
		var testRunId: UUID,

		@ApiModelProperty("This is the name of the sut")
		@JsonProperty("sut_filename")
		@Column(name = "sut_filename")
		val sutFilename: String?,

		@ApiModelProperty("This is the test set id in the Axini Test Manager (as seen in the url).", example = "3ec4d0e1-2431-4516-ab3e-f15b061603e3")
		@JsonProperty("testset_id")
		@Type(type = "uuid-char")
		var testsetId: UUID,

		@Type(type = "text")
		val cre_cr3: String,

		@Type(type = "text")
		val cre_err3: String,

		@Type(type = "text")
		val cre_mux_err3: String
) : AbstractModel()