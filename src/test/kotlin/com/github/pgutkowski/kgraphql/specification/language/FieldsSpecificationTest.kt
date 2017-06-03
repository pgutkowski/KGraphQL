package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.*
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test

@Specification("2.5 Fields")
class FieldsSpecificationTest {

    data class ActorWrapper(val id : String, val actualActor: Actor)

    val age = 432

    val schema = defaultSchema {
        query {
            name = "actor"
            resolver { -> ActorWrapper("BLinda", Actor("Boguś Linda", age)) }
        }
    }

    @Test
    fun `field may itself contain a selection set`() {
        val response = deserialize(schema.execute("{actor{id, actualActor{name, age}}}"))
        val map = extract<Map<String, Any>>(response, "data/actor/actualActor")
        MatcherAssert.assertThat(map, CoreMatchers.equalTo(mapOf("name" to "Boguś Linda", "age" to age)))
    }
}

