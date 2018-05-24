package nl.utwente.axini.atana.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


class ZonedDateTimeSerializer : JsonSerializer<LocalDateTime>() {
	@Throws(IOException::class, JsonProcessingException::class)
	override fun serialize(arg0: LocalDateTime, arg1: JsonGenerator, arg2: SerializerProvider) {
		arg1.writeString(arg0.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")))
	}
}

class ZonedDateTimeDeserializer : JsonDeserializer<LocalDateTime>() {
	@Throws(IOException::class, JsonProcessingException::class)
	override fun deserialize(arg0: JsonParser, arg1: DeserializationContext): LocalDateTime {
		return try {
			ZonedDateTime.parse(arg0.text, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
		} catch (e: DateTimeParseException) {
			return try {
				ZonedDateTime.parse(arg0.text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"))
			} catch (e2: DateTimeParseException) {
				ZonedDateTime.parse(arg0.text)
			}.toLocalDateTime()
		}.toLocalDateTime()
	}
}