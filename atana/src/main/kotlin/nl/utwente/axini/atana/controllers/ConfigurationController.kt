package nl.utwente.axini.atana.controllers

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import nl.utwente.axini.atana.logger
import nl.utwente.axini.atana.models.Configuration
import nl.utwente.axini.atana.service.ConfigurationService
import nl.utwente.axini.atana.validation.validate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@RestController
class ConfigurationController(val configurationService: ConfigurationService) : AbstractController() {
	override val log by logger()

	@PostMapping("/config")
	@ApiOperation("Submit a new configuration")
	fun setConfig(@ApiParam("A configuration object with key value pairs") @RequestBody config: Configuration) {
		validate(config) //Generic validation of the data
		configurationService.config = config
		configurationService.freshConfig.set(true)
		log.debug("Setting config to %s".format(config))
	}

	@GetMapping("/config")
	@ApiOperation("Show the current configuration")
	fun showConfig() = configurationService.config
}