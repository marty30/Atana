package nl.utwente.axini.atana.validation

import nl.utwente.axini.atana.controllers.InvalidRequestDataException
import javax.validation.Validation


@Throws(InvalidRequestDataException::class)
fun validate(vararg objectToValidates: Any) {
    val validator = Validation.buildDefaultValidatorFactory().validator
    objectToValidates.forEach {
        val violations = validator.validate(it)
        if (violations.isNotEmpty()) {
            throw InvalidRequestDataException("The following violations occurred: " + violations.map { "${it.propertyPath} ${it.message}" }.toString())
        }
    }
}

/**
 * Throws an [InvalidRequestDataException] with the result of calling [lazyMessage] if the [value] is false.
 */
@Throws(InvalidRequestDataException::class)
inline fun validateData(value: Boolean, lazyMessage: () -> Any) {
    if (!value) {
        val message = lazyMessage()
        throw InvalidRequestDataException(message.toString())
    }
}