package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.Actor
import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.assertNoErrors
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.executeEqualQueries
import com.github.pgutkowski.kgraphql.extract
import com.github.pgutkowski.kgraphql.extractOrNull
import com.github.pgutkowski.kgraphql.integration.BaseSchemaTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

@Specification("2.8 Fragments")
class FragmentsSpecificationTest {

    val age = 232

    val actorName = "Boguś Linda"

    val id = "BLinda"

    data class ActorWrapper(val id : String, val actualActor: Actor)

    val schema = defaultSchema {
        query("actor") {
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
    fun `Inline fragments may also be used to apply a directive to a group of fields`(){
        val response = deserialize(schema.execute (
                "query (\$expandedInfo : Boolean){actor{actualActor{name ... @include(if: \$expandedInfo){ age }}}}",
                "{\"expandedInfo\":false}"
        ))
        assertNoErrors(response)
        assertThat(extractOrNull(response, "data/actor/actualActor/name"), equalTo("Boguś Linda"))
        assertThat(extractOrNull(response, "data/actor/actualActor/age"), nullValue())
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
