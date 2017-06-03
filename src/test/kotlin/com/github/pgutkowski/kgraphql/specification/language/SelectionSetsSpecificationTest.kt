package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

@Specification("2.4 Selection Sets")
class SelectionSetsSpecificationTest {

    val age = 432

    val schema = defaultSchema {
        query {
            name = "actor"
            resolver { -> Actor("Boguś Linda", age) }
        }
    }

    @Test
    fun `operation selects the set of information it needs`() {
        val response = deserialize(schema.execute("{actor{name, age}}"))
        val map = extract<Map<String, Any>>(response, "data/actor")
        assertThat(map, equalTo(mapOf("name" to "Boguś Linda", "age" to age)))
    }

    @Test
    fun `operation selects the set of information it needs 2`() {
        val response = deserialize(schema.execute("{actor{name}}"))
        val map = extract<Map<String, Any>>(response, "data/actor")
        assertThat(map, equalTo(mapOf<String, Any>("name" to "Boguś Linda")))
    }

    @Test
    fun `operation selects the set of information it needs 3`() {
        val response = deserialize(schema.execute("{actor{age}}"))
        val map = extract<Map<String, Any>>(response, "data/actor")
        assertThat(map, equalTo(mapOf<String, Any>("age" to age)))
    }

}