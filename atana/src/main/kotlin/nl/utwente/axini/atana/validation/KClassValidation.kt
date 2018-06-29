package nl.utwente.axini.atana.validation

import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.Payload
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

@MustBeDocumented
@Constraint(validatedBy = [(SubclassOfValidator::class)])
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SubclassOf(val value: KClass<out Any>, val message: String = "{nl.utwente.axini.atana.constraint.SubclassOf.message}", val groups: Array<KClass<*>> = arrayOf(), val payload: Array<KClass<out Payload>> = arrayOf())


class SubclassOfValidator : ConstraintValidator<SubclassOf, KClass<out Any>> {
    private lateinit var expectedClass: KClass<out Any>
    override fun initialize(constraintAnnotation: SubclassOf) {
        expectedClass = constraintAnnotation.value
    }

    override fun isValid(value: KClass<out Any>, context: ConstraintValidatorContext?): Boolean {
        return value.isSubclassOf(expectedClass)
    }
}