package nl.utwente.axini.atana.controllers

import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Example
import io.swagger.annotations.ExampleProperty
import nl.utwente.axini.atana.repository.TestModelRepository
import nl.utwente.axini.atana.repository.TestRunRepository
import nl.utwente.axini.atana.service.GraphService
import nl.utwente.axini.atana.validation.validate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class GraphController(val testModelRepository: TestModelRepository, val testRunRepository: TestRunRepository, val graphService: GraphService) : AbstractController() {

	@GetMapping("/graph/{id}", produces = ["image/svg+xml", "application/xml", "text/xml"])
	fun graphSvg(
			@ApiParam("The id of the test run to be plotted",
					examples = Example(ExampleProperty("1"), ExampleProperty("All")))
			@PathVariable("id") id: String): String {
		validate(id) //Generic validation of the data
		val graph = graphService.buildGraph(testModelRepository.findAllByTestRunId(UUID.fromString(id)).first(), testRunRepository.findAllByTestRunId(UUID.fromString(id)).map { it.testCases }.flatten())
		return Graphviz.fromGraph(graph).render(Format.SVG).toString()
	}
}