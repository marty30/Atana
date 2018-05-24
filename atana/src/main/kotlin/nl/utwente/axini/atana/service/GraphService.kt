package nl.utwente.axini.atana.service

import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.model.Factory
import guru.nidi.graphviz.model.Graph
import guru.nidi.graphviz.model.Link
import guru.nidi.graphviz.model.Node
import nl.utwente.axini.atana.models.TestCase
import nl.utwente.axini.atana.models.TestModel
import nl.utwente.axini.atana.models.TestResult
import org.springframework.stereotype.Service

@Service
class GraphService {
	fun buildGraph(model: TestModel, testcases: Collection<TestCase>): Graph {
		val nodes = model.stss.asSequence().map { it.states }.flatten().map { Pair(it.id, Factory.node(it.id)) }.toMap()
		val transitions = model.stss.asSequence().map { it.transitions }.flatten().map { Pair(it.attributes.label, Pair(it, Label.of(it.attributes.label))) }.toMap()
		val tauTransitions = model.stss.asSequence().map { it.transitions }.flatten().filter { it -> it.attributes.label.isEmpty() }.toList()

		val steps = testcases.asSequence().map { Pair(it.steps, it.verdict) }.flatMap {
			it.first.map { i -> Triple(i, if (it.second == TestResult.PASSED) 1 else 0, if (it.second == TestResult.FAILED) 1 else 0) }.asSequence()
		}.groupBy { it.first.fullLabel }.map {
			Triple(it.key, it.value.sumBy { it.second }, it.value.sumBy { it.third })
		}

		val failedCount = testcases.filter { it.verdict == TestResult.FAILED }.count()

		var tauLinks = mutableMapOf<Pair<String, String>, Node?>()
		val links = steps.map { (step, passed_count, failed_count) ->
			val (transition, label) = transitions[step] ?: Pair(null, null)
			val edgeColor = calculateColor(passed_count, failed_count, failedCount)
			val tauTransition = tauTransitions.find { it.source == transition?.source || it.target == transition?.target || it.target == transition?.source || it.source == transition?.target }
			if (tauTransition != null) {
				tauLinks[Pair(tauTransition.target, tauTransition.source)] = nodes[tauTransition.target]?.link(Link.to(nodes[tauTransition.source]).with(edgeColor))
			}
			nodes[transition?.target]?.link(Link.to(nodes[transition?.source]).with(label, edgeColor))
		}.filter { it != null }.toList()

		//Remove tau transitions that are not really in use (are not connected on both sides)
		tauLinks = tauLinks.filter {
			(
					it.key.first in steps.map { transitions[it.first]?.first?.source }
							|| it.key.first in steps.map { transitions[it.first]?.first?.target }
							|| it.key.first in tauTransitions.map { it.source }
					) && (
					it.key.second in steps.map { transitions[it.first]?.first?.source }
							|| it.key.second in steps.map { transitions[it.first]?.first?.target }
							|| it.key.second in tauTransitions.map { it.target }
					)

		}.toMutableMap()
		return Factory.graph("overview").directed().with(*links.toTypedArray(), *tauLinks.values.toTypedArray())
	}

	private val min = Triple(0.0, 0.1, 0.1)
	private val max = Triple(0.0, 1.0, 1.0)
	private fun calculateColor(passed_count: Int, failed_count: Int, total_count: Int): Color {
		val (h, s, v) = Triple(
				(max.first - min.first) * Integer.max(failed_count - passed_count, 0) / total_count + min.first,
				(max.second - min.second) * Integer.max(failed_count - passed_count, 0) / total_count + min.second,
				(max.third - min.third) * Integer.max(failed_count - passed_count, 0) / total_count + min.third
		)
		return Color.hsv(h, s, v)
	}
}