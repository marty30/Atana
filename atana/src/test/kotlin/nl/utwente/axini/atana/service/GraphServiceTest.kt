package nl.utwente.axini.atana.service

import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory.graph
import guru.nidi.graphviz.model.Factory.node
import guru.nidi.graphviz.model.Link
import nl.utwente.axini.atana.models.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.util.*
import guru.nidi.graphviz.attribute.Label as GraphLabel


class GraphServiceTest {
	@Test
	fun `build a graph from two test runs and a model`() {
		//Given
		val id = UUID.randomUUID()
		val model = TestModel(id, null, setOf(
				Sts("example", setOf(
						State("state 0", StateAttribute("0", "node", null)),
						State("state 1", StateAttribute("1", "node", null)),
						State("state 2", StateAttribute("2", "node", null)),
						State("state 3", StateAttribute("3", "node", null)),
						State("state 4", StateAttribute("4", "node", null)),
						State("state 5", StateAttribute("5", "node", null)),
						State("state 6", StateAttribute("6", "node", null))
				), setOf(), setOf(StartState("state 1", null)), setOf(
						Transition("state 0", "state 1", TransitionAttribute("!init", "out", null)),
						Transition("state 1", "state 2", TransitionAttribute("?a", "in", null)),
						Transition("state 1", "state 3", TransitionAttribute("?b", "in", null)),
						Transition("state 2", "state 4", TransitionAttribute("!a", "out", null)),
						Transition("state 3", "state 4", TransitionAttribute("!b", "out", null)),
						Transition("state 4", "state 5", TransitionAttribute("!c", "out", null)),
						Transition("state 4", "state 6", TransitionAttribute("!d", "out", null))
				), setOf(), null, null, null)
		), null)

		val testRun = TestRun(id, setOf(
				TestCase(0, null, TestResult.PASSED, null, setOf(
						Step(Label("init", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("a", "in", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("a", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("c", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0)
				), 4),
				TestCase(1, null, TestResult.PASSED, null, setOf(
						Step(Label("init", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("a", "in", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("a", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("d", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0)
				), 4),
				TestCase(2, null, TestResult.FAILED, null, setOf(
						Step(Label("init", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("b", "in", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("b", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("c", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0)
				), 4),
				TestCase(3, null, TestResult.FAILED, null, setOf(
						Step(Label("init", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("b", "in", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("b", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("d", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0)
				), 4)
		))

		val node_0 = node("state 0")
		val node_1 = node("state 1")
		val node_2 = node("state 2")
		val node_3 = node("state 3")
		val node_4 = node("state 4")
		val node_5 = node("state 5")
		val node_6 = node("state 6")

		val expectedGraph = graph("overview").directed().with(
				node_1.link(Link.to(node_0).with(GraphLabel.of("!init"), Color.hsv(0.0, 0.1, 0.1))),
				node_2.link(Link.to(node_1).with(GraphLabel.of("?a"), Color.hsv(0.0, 0.1, 0.1))),
				node_4.link(Link.to(node_2).with(GraphLabel.of("!a"), Color.hsv(0.0, 0.1, 0.1))),
				node_5.link(Link.to(node_4).with(GraphLabel.of("!c"), Color.hsv(0.0, 0.1, 0.1))),
				node_6.link(Link.to(node_4).with(GraphLabel.of("!d"), Color.hsv(0.0, 0.1, 0.1))),
				node_4.link(Link.to(node_3).with(GraphLabel.of("!b"), Color.hsv(0.0, 1.0, 1.0))),
				node_3.link(Link.to(node_1).with(GraphLabel.of("?b"), Color.hsv(0.0, 1.0, 1.0)))
		)

		//When
		val calculatedGraph = GraphService().buildGraph(model, testRun.testCases)

		//Then
		assertEquals(Graphviz.fromGraph(expectedGraph).render(Format.PLAIN).toString(), Graphviz.fromGraph(calculatedGraph).render(Format.PLAIN).toString())
	}

	@Test
	fun `build graph with tau transitions`() {
		//Given
		val id = UUID.randomUUID()
		val model = TestModel(id, null, setOf(
				Sts("example", setOf(
						State("state 0", StateAttribute("0", "node", null)),
						State("state 1", StateAttribute("1", "node", null)),
						State("state 2", StateAttribute("2", "node", null)),
						State("state 3a", StateAttribute("3a", "node", null)),
						State("state 3b", StateAttribute("3b", "node", null)),
						State("state 4", StateAttribute("4", "node", null)),
						State("state 5", StateAttribute("5", "node", null)),
						State("state 6", StateAttribute("6", "node", null)),
						State("unused", StateAttribute("unused", "node", null))
				), setOf(), setOf(StartState("state 1", null)), setOf(
						Transition("state 0", "state 1", TransitionAttribute("!init", "out", null)),
						Transition("state 1", "state 2", TransitionAttribute("?a", "in", null)),
						Transition("state 1", "state 3a", TransitionAttribute("?b", "in", null)),
						Transition("state 3a", "state 3b", TransitionAttribute("", "tau", null)),
						Transition("state 2", "state 4", TransitionAttribute("!a", "out", null)),
						Transition("state 3b", "state 4", TransitionAttribute("!b", "out", null)),
						Transition("state 4", "state 5", TransitionAttribute("!c", "out", null)),
						Transition("state 4", "state 6", TransitionAttribute("!d", "out", null)),
						Transition("state 0", "unused", TransitionAttribute("", "tau", null))
				), setOf(), null, null, null)
		), null)

		val testRun = TestRun(id, setOf(
				TestCase(0, null, TestResult.PASSED, null, setOf(
						Step(Label("init", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("a", "in", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("a", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("c", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0)
				), 4),
				TestCase(1, null, TestResult.PASSED, null, setOf(
						Step(Label("init", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("a", "in", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("a", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("d", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0)
				), 4),
				TestCase(2, null, TestResult.FAILED, null, setOf(
						Step(Label("init", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("b", "in", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("b", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("c", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0)
				), 4),
				TestCase(3, null, TestResult.FAILED, null, setOf(
						Step(Label("init", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("b", "in", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("b", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0),
						Step(Label("d", "out", "pos"), timestamp = LocalDateTime.now(), stepNumber = 0)
				), 4)
		))

		val node_0 = node("state 0")
		val node_1 = node("state 1")
		val node_2 = node("state 2")
		val node_3a = node("state 3a")
		val node_3b = node("state 3b")
		val node_4 = node("state 4")
		val node_5 = node("state 5")
		val node_6 = node("state 6")

		val expectedGraph = graph("overview").directed().with(
				node_1.link(Link.to(node_0).with(GraphLabel.of("!init"), Color.hsv(0.0, 0.1, 0.1))),
				node_2.link(Link.to(node_1).with(GraphLabel.of("?a"), Color.hsv(0.0, 0.1, 0.1))),
				node_4.link(Link.to(node_2).with(GraphLabel.of("!a"), Color.hsv(0.0, 0.1, 0.1))),
				node_5.link(Link.to(node_4).with(GraphLabel.of("!c"), Color.hsv(0.0, 0.1, 0.1))),
				node_6.link(Link.to(node_4).with(GraphLabel.of("!d"), Color.hsv(0.0, 0.1, 0.1))),
				node_4.link(Link.to(node_3b).with(GraphLabel.of("!b"), Color.hsv(0.0, 1.0, 1.0))),
				node_3b.link(Link.to(node_3a).with(Color.hsv(0.0, 1.0, 1.0))),
				node_3a.link(Link.to(node_1).with(GraphLabel.of("?b"), Color.hsv(0.0, 1.0, 1.0)))
		)

		//When
		val calculatedGraph = GraphService().buildGraph(model, testRun.testCases)

		Graphviz.fromGraph(expectedGraph).render(Format.PNG).toFile(File("example/ex1.png"))
		Graphviz.fromGraph(calculatedGraph).render(Format.PNG).toFile(File("example/cal1.png"))

		//Then
		assertEquals(Graphviz.fromGraph(expectedGraph).render(Format.PLAIN).toString(), Graphviz.fromGraph(calculatedGraph).render(Format.PLAIN).toString())
	}
}