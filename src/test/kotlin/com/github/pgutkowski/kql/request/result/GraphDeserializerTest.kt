package com.github.pgutkowski.kql.request.result

import com.github.pgutkowski.kql.request.Graph
import com.github.pgutkowski.kql.request.serialization.DefaultGraphDeserializer
import junit.framework.Assert.assertEquals
import org.junit.Test


class GraphDeserializerTest {

    val deserializer = DefaultGraphDeserializer()

    @Test
    fun nestedQueryDeserialization() {
        val map = deserializer.deserialize("{hero{name, appearsIn}\nvillain{name, [deeds]}}")
        val expected = Graph(
                "hero" to MultiMapWithNulls("name", "appearsIn"),
                "villain" to MultiMapWithNulls("name", "[deeds]")
        )

        assertEquals(expected, map)
    }

    @Test
    fun doubleNestedQueryDeserialization() {
        val map = deserializer.deserialize("{hero{name, appearsIn{title, year}}\nvillain{name, [deeds]}}")
        val expected = Graph(
                "hero" to Graph("name" to null, "appearsIn" to MultiMapWithNulls("title", "year")),
                "villain" to MultiMapWithNulls("name", "[deeds]")
        )

        assertEquals(expected, map)
    }

    @Test
    fun tripleNestedQueryDeserialization() {
        val map = deserializer.deserialize("{hero{name, appearsIn{title{abbr, full}, year}}\nvillain{name, [deeds]}}")
        val expected = Graph(
                "hero" to Graph("name" to null, "appearsIn" to Graph(
                        "title" to MultiMapWithNulls("abbr", "full"),
                        "year" to null)),
                "villain" to MultiMapWithNulls("name", "[deeds]")
        )

        assertEquals(expected, map)
    }

    @Test
    fun testIndexOfClosingBracket(){
        fun executeTest(input : String, expected: Int? = null){
            val result = deserializer.indexOfClosingBracket(input)
            assertEquals(expected ?: input.length -1, result)
        }
        executeTest("{hero{name, appearsIn{title, year}}}")
        executeTest("{hero{name{first, second}, appearsIn{title, year}}}")
        executeTest("{hero}")

        executeTest("{hero}, movie{year, title}}", 5)
        executeTest("{hero{powers{primary}}}\nvillain{deeds}}", 22)
    }

    fun MultiMapWithNulls(vararg keys: String) : Graph {
        return Graph(keys.associate { it to null })
    }
}