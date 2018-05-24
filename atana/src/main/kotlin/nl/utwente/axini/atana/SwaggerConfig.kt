package nl.utwente.axini.atana

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@EnableSwagger2
class SwaggerConfig {
	@Bean
	fun api() = Docket(DocumentationType.SWAGGER_2)
			.apiInfo(apiInfo())
			.select()
			.apis(RequestHandlerSelectors.basePackage("nl.utwente.axini.atana.controllers"))
			.paths(PathSelectors.any())
			.build()!!

	fun apiInfo() = ApiInfoBuilder()
			.title("Atana - Axini Test ANAlyser")
			.description("A test analyser skeleton that will work with the supplied grouping and analysis service")
			.version(mavenModel.value.version)
			.contact(Contact("Martijn Willemsen", null, "m.j.willemsen@student.utwente.nl"))
			.build()!!
}

@Controller
class SwaggerController {
	/**
	 * This controller allows users to access the swagger ui from the root of the application.
	 */
	@RequestMapping("/")
	fun forwardToSwagger(): String {
		return "redirect:/swagger-ui.html"
	}
}
