package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.Actor
import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

@Specification("2.7 Field Alias")
class FieldAliasSpecificationTest {

    val age = 232

    val actorName = "Boguś Linda"

    val schema = defaultSchema {
        query("actor") {
            resolver { -> Actor(actorName, age) }
        }

        type<Actor>{
            transformation(Actor::age) {
                age: Int , inMonths : Boolean? -> if(inMonths == true) age * 12 else age
            }
        }
    }

    @Test
    fun `can define response object field name`(){
        val map = deserialize(schema.execute("{actor{ageMonths: age(inMonths : true) ageYears: age(inMonths : false)}}"))
        assertThat(map.extract<Int>("data/actor/ageMonths"), equalTo(age * 12))
        assertThat(map.extract<Int>("data/actor/ageYears"), equalTo(age))
    }

    @Test
    fun `top level of a query can be given alias`(){
        val map = deserialize(schema.execute("{ boguś : actor{name}}"))
        assertThat(map.extract<String>("data/boguś/name"), equalTo(actorName))
    }
}