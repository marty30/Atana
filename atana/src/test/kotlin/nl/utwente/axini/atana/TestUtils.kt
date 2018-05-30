package nl.utwente.axini.atana

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.junit.Assert.assertEquals
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.mock.http.MockHttpOutputMessage
import org.springframework.stereotype.Component
import org.springframework.test.web.client.RequestMatcher
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate



private fun filterMap(map: MutableMap<*, *>?, vararg keysToRemove: String) {
	if (map == null || map.isEmpty() || keysToRemove.isEmpty()) {
		return
	}

	for (k in keysToRemove) {
		map.remove(k)
	}

	for (entry in map.entries) {
		when {
			entry.value is Map<*, *> -> {
				filterMap(entry.value as MutableMap<*, *>, *keysToRemove)
			}
			entry.value is Collection<*> -> (entry.value as Collection<*>).forEach {
				if (it is Map<*, *>)
					filterMap(it as MutableMap<*, *>, *keysToRemove)
				else
					println("Cannot remove a key from a collection that does not contain a map")
			}
		}
	}
}

fun jsonEquals(expected: Any?, vararg propertiesToIgnore: String): Matcher<Any?> {
	val expectedJson: MutableMap<*, *> = jsonObjectMapper().convertValue(expected, MutableMap::class.java)
	filterMap(expectedJson, *propertiesToIgnore)
	return object : TypeSafeDiagnosingMatcher<Any?>() {
		override fun describeTo(description: Description) {
			description.appendText("expected the json string to equal ").appendValue(expectedJson)
		}

		override fun matchesSafely(actual: Any?, mismatchDescription: Description): Boolean {
			val actualJson: MutableMap<*, *> = jsonObjectMapper().convertValue(actual, MutableMap::class.java)
			filterMap(actualJson, *propertiesToIgnore)
			mismatchDescription.appendText(" was ").appendValue(actualJson)
			return actual == expected || actualJson == expectedJson
		}
	}
}

inline fun <reified T : Any> contentJson(expected: Any?) = RequestMatcher {
	val actual = jsonObjectMapper().readValue((it as MockHttpOutputMessage).bodyAsString, T::class.java)
	assertEquals("Request content did not match.", jsonObjectMapper().writeValueAsString(expected), jsonObjectMapper().writeValueAsString(actual))
}

fun containsIgnoringCase(expected: String): Matcher<String?>? {
	return object : TypeSafeDiagnosingMatcher<String>() {
		override fun describeTo(description: Description) {
			description.appendText("containing substring \"$expected\"")
		}

		override fun matchesSafely(actual: String?, mismatchDescription: Description): Boolean {
			mismatchDescription.appendText(" was ").appendValue(actual)
			if (actual == null) {
				return actual == expected
			}
			return actual.toLowerCase().contains(expected.toLowerCase())
		}
	}
}