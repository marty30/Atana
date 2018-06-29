package nl.utwente.axini.atana.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import kotlin.reflect.KClass


class KClassSerializer : JsonSerializer<KClass<*>>() {
    override fun serialize(value: KClass<*>, g: JsonGenerator, provider: SerializerProvider) {
        g.writeString(value.java.name)
    }
}

class KClassDeserializer : JsonDeserializer<KClass<*>>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): KClass<*> {
        val value = p?.readValueAs(String::class.java)
        return Class.forName(value).kotlin
    }
}
