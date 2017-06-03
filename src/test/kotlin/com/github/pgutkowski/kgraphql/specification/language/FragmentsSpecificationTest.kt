package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.*
import com.github.pgutkowski.kgraphql.integration.BaseSchemaTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Specification("2.8 Fragments")
class FragmentsSpecificationTest {

    val age = 232

    val actorName = "Boguś Linda"

    val id = "BLinda"

    data class ActorWrapper(val id : String, val actualActor: Actor)

    val schema = defaultSchema {
        query {
            name = "actor"
            resolver { -> ActorWrapper(id, Actor(actorName, age)) }
        }
    }

    val BaseTestSchema = object : BaseSchemaTest(){}

    @Test
    fun `fragment's fields are added to the query at the same level as the fragment invocation`(){
        val expected = mapOf("data" to mapOf(
                "actor" to mapOf(
                        "id" to id,
                        "actualActor" to mapOf("name" to actorName, "age" to age)
                ))
        )
        executeEqualQueries(schema,
                expected,
                "{actor{id, actualActor{name, age}}}",
                "{actor{ ...actWrapper}} fragment actWrapper on ActorWrapper {id, actualActor{ name, age }}"
        )
    }

    @Test
    fun `fragments can be nested`(){
        val expected = mapOf("data" to mapOf(
                "actor" to mapOf(
                        "id" to id,
                        "actualActor" to mapOf("name" to actorName, "age" to age)
                ))
        )
        executeEqualQueries(schema,
                expected,
                "{actor{id, actualActor{name, age}}}",
                "{actor{ ...actWrapper}} fragment act on Actor{name, age} fragment actWrapper on ActorWrapper {id, actualActor{ ...act }}"
        )
    }

    @Test
    @Disabled("Directived are not implemented yet")
    fun `Inline fragments may also be used to apply a directive to a group of fields`(){
        schema.execute("{actor{name ... @include(if: \$expandedInfo){ age }}}", "{\"expandedInfo\":true}")
    }

    @Test
    fun `query with inline fragment with type condition`(){
        val map = BaseTestSchema.execute("{people{name, age, ... on Actor {isOld} ... on Director {favActors{name}}}}")
        assertNoErrors(map)
        for(i in extract<List<*>>(map, "data/people").indices){
            val name = extract<String>(map, "data/people[$i]/name")
            when(name){
                "David Fincher" /* director */  ->{
                    MatcherAssert.assertThat(extract<List<*>>(map, "data/people[$i]/favActors"), CoreMatchers.notNullValue())
                    MatcherAssert.assertThat(extractOrNull<Boolean>(map, "data/people[$i]/isOld"), CoreMatchers.nullValue())
                }
                "Brad Pitt" /* actor */ -> {
                    MatcherAssert.assertThat(extract<Boolean>(map, "data/people[$i]/isOld"), CoreMatchers.notNullValue())
                    MatcherAssert.assertThat(extractOrNull<List<*>>(map, "data/people[$i]/favActors"), CoreMatchers.nullValue())
                }
            }
        }
    }

    @Test
    fun `query with external fragment with type condition`(){
        val map = BaseTestSchema.execute("{people{name, age ...act ...dir}} fragment act on Actor {isOld} fragment dir on Director {favActors{name}}")
        assertNoErrors(map)
        for(i in extract<List<*>>(map, "data/people").indices){
            val name = extract<String>(map, "data/people[$i]/name")
            when(name){
                "David Fincher" /* director */  ->{
                    MatcherAssert.assertThat(extract<List<*>>(map, "data/people[$i]/favActors"), CoreMatchers.notNullValue())
                    MatcherAssert.assertThat(extractOrNull<Boolean>(map, "data/people[$i]/isOld"), CoreMatchers.nullValue())
                }
                "Brad Pitt" /* actor */ -> {
                    MatcherAssert.assertThat(extract<Boolean>(map, "data/people[$i]/isOld"), CoreMatchers.notNullValue())
                    MatcherAssert.assertThat(extractOrNull<List<*>>(map, "data/people[$i]/favActors"), CoreMatchers.nullValue())
                }
            }
        }
    }
}
