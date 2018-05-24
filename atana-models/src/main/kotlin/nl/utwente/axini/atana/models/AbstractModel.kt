package nl.utwente.axini.atana.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass
import javax.validation.constraints.Min

@MappedSuperclass
abstract class AbstractModel(
		@Id
		@GeneratedValue
		@JsonProperty(required = false)
		@ApiModelProperty(hidden = true)
		@Min(0)
		var databaseId: Long? = null,

		@CreationTimestamp
		@JsonProperty(required = false)
		@ApiModelProperty(hidden = true)
		var createdAt: LocalDateTime = LocalDateTime.now(),

		@UpdateTimestamp
		@JsonProperty(required = false)
		@ApiModelProperty(hidden = true)
		var updatedAt: LocalDateTime = LocalDateTime.now()
)