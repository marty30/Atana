package nl.utwente.axini.atana.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer
import com.fasterxml.jackson.databind.ser.std.UUIDSerializer
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory
import guru.nidi.graphviz.model.Link
import guru.nidi.graphviz.model.MutableGraph
import io.swagger.annotations.ApiModelProperty
import org.hibernate.annotations.Type
import java.io.File
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.validation.Valid
import javax.validation.constraints.NotEmpty

val jsonObjectMapper = lazy {
	jacksonObjectMapper()
			.registerModule(JavaTimeModule())
			.registerModule(Jdk8Module())
			.registerModule(ParameterNamesModule())
			.findAndRegisterModules()
}

@Entity
data class TestModel(
		@ApiModelProperty(example = "3ec4d0e1-2431-4516-ab3e-f15b061603e3")
		@JsonProperty("test_run_id", required = false)
		@JsonSerialize(using = UUIDSerializer::class)
		@JsonDeserialize(using = UUIDDeserializer::class)
		@Type(type = "uuid-char")
		var testRunId: UUID?,

		@JsonProperty(required = false)
		var mutant: String?,

		@OneToMany(cascade = [CascadeType.ALL])
		@get:Valid
		@get:NotEmpty
		val stss: Set<Sts>,

		@JsonProperty("testcase_id", required = false)
		@JsonAlias("testcase_id", "testcaseId")
		val testcaseId: Int?

) : AbstractModel() {
	fun cleanup() {
		stss.forEach {
			it.transitions.forEach {
				it.attributes.label = it.attributes.label.replace("\n", " ")
			}
		}
	}

	fun export(type: ExportType, targetFile: File? = null): File {
		val f = targetFile ?: File.createTempFile(this.testRunId.toString() + "model", "." + type.name.toLowerCase())
		val g: MutableGraph = Factory.mutGraph()
		g.isDirected = true
		stss.asSequence().forEach { sts ->
			val cg = Factory.mutGraph(sts.sts ?: sts.name)
			val children = sts.children.map { it.id to it.attributes.label }.toMap()
			sts.transitions.forEach {
				val s = Factory.node(it.source)
				val t = if (it.target in children.keys) {
					val label = children[it.target]
					val childSts = stss.find { sts ->
						if (sts.sts == null)
							sts.name == label
						else
							sts.sts.startsWith("_${it.target}_")
					}!!
					val newTarget = childSts.startStates.first().id
					Factory.node(newTarget).with(guru.nidi.graphviz.attribute.Label.of(childSts.states.find { st -> st.id == newTarget }?.attributes?.label
							?: newTarget))
				} else {
					Factory.node(it.target).with(guru.nidi.graphviz.attribute.Label.of(sts.states.find { st -> st.id == it.target }?.attributes?.label
							?: it.target))
				}
				val l = s.link(Link.to(t).with(guru.nidi.graphviz.attribute.Label.of(it.attributes.label)))
				cg.add(l)
			}
			g.add(cg)
		}

		when (type) {
			ExportType.DOT -> Graphviz.fromGraph(g).render(Format.XDOT).toFile(f)
			ExportType.PNG -> Graphviz.fromGraph(g).render(Format.PNG).toFile(f)
			ExportType.SVG -> Graphviz.fromGraph(g).render(Format.SVG_STANDALONE).toFile(f)
		}
		return f
	}
}

@Entity
data class TraceProperties (
		@OneToMany(cascade = [CascadeType.ALL])
		@get:Valid
		@JsonProperty(required = false)
		val states: Set<State>,
		@OneToMany(cascade = [CascadeType.ALL])
		@get:Valid
		@JsonProperty(required = false)
		val last_states: Set<State>,

		val passed: Boolean?
): AbstractModel()

enum class ExportType {
	DOT, PNG, SVG
}

@Entity
data class Sts(
		val name: String,

		@OneToMany(cascade = [CascadeType.ALL])
		@get:Valid
		@get:NotEmpty
		val states: Set<State>,

		@OneToMany(cascade = [CascadeType.ALL])
		@get:Valid
		val children: Set<ChildModel>,

		@JsonProperty("start_states")
		@OneToMany(cascade = [CascadeType.ALL])
		@get:Valid
		@get:NotEmpty
		val startStates: Set<StartState>,

		@OneToMany(cascade = [CascadeType.ALL])
		@get:Valid
		@get:NotEmpty
		val transitions: Set<Transition>,

		@OneToMany(cascade = [CascadeType.ALL])
		@get:Valid
		val return_transitions: Set<Transition>,

		val return_state: String?,

		val sts: String?,

		@OneToOne(cascade = [CascadeType.ALL])
		@get:Valid
		@JsonProperty(required = false)
		val trace_properties: TraceProperties?
) : AbstractModel()

@Entity
data class State(
		@get:NotEmpty
		val id: String,

		@OneToOne(cascade = [CascadeType.ALL])
		@get:Valid
		val attributes: StateAttribute
) : AbstractModel()

@Entity
data class StartState(
		val id: String,

		@JsonProperty(required = false)
		val covered: Boolean?
) : AbstractModel()

@Entity
data class StateAttribute(
		val label: String,

		val type: String,

		@JsonProperty(required = false)
		val covered: Boolean?
) : AbstractModel()

@Entity
data class ChildModel(
		val id: String,

		@OneToOne(cascade = [CascadeType.ALL])
		@get:Valid
		val attributes: ChildModelAttribute
) : AbstractModel()

@Entity
data class ChildModelAttribute(
		val label: String,

		val type: String,

		@JsonProperty("hex_id")
		val hexId: String,

		val partially_covered: Boolean?
) : AbstractModel()

@Entity
data class Transition(
		val source: String,

		val target: String,

		@OneToOne(cascade = [CascadeType.ALL])
		@get:Valid
		val attributes: TransitionAttribute
) : AbstractModel()

@Entity
data class TransitionAttribute(
		var label: String,

		@JsonProperty(required = false)
		val type: String?,

		@JsonProperty(required = false)
		val covered: Boolean?
) : AbstractModel()