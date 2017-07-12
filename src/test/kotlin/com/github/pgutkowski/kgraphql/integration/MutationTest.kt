package com.github.pgutkowski.kgraphql.integration

import com.github.pgutkowski.kgraphql.Actor
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.assertNoErrors
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class MutationTest : BaseSchemaTest() {

    val testActor = Actor("Michael Caine", 72)

    @Test
    fun `simple mutation multiple fields`(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){name, age}}")
        assertNoErrors(map)
        assertThat(map.extract<Map<String, Any>>("data/createActor"), equalTo(mapOf("name" to testActor.name, "age" to testActor.age)))
    }

    @Test
    fun `simple mutation single field`(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){name}}")
        assertNoErrors(map)
        assertThat(map.extract<Map<String, Any>>("data/createActor"), equalTo(mapOf<String, Any>("name" to testActor.name)))
    }

    @Test
    fun `simple mutation single field 2`(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){age}}")
        assertNoErrors(map)
        assertThat(map.extract<Map<String, Any>>("data/createActor"), equalTo(mapOf<String, Any>("age" to testActor.age)))
    }

    @Test
    fun `invalid mutation name`(){
        expect<RequestException>("property createBanana on Mutation does not exist"){
            execute("mutation {createBanana(name: \"${testActor.name}\", age: ${testActor.age}){age}}")
        }
    }

    @Test
    fun `invalid argument type`(){
        expect<RequestException>("argument '\"fwfwf\"' is not valid value of type Int"){
            execute("mutation {createActor(name: \"${testActor.name}\", age: \"fwfwf\"){age}}")
        }
    }

    @Test
    fun `invalid arguments number`(){
        expect<RequestException>("createActor does support arguments [name, age]. Found arguments [name, bananan, age]"){
            execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}, bananan: \"fwfwf\"){age}}")
        }
    }

    @Test
    fun `mutation with alias`(){
        val map = execute("{caine : createActor(name: \"${testActor.name}\", age: ${testActor.age}){age}}")
        assertNoErrors(map)
        assertThat(map.extract<Map<String, Any>>("data/caine"), equalTo(mapOf<String, Any>("age" to testActor.age)))
    }

    @Test
    fun `mutation with field alias`(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){howOld: age}}")
        assertNoErrors(map)
        assertThat(map.extract<Map<String, Any>>("data/createActor"), equalTo(mapOf<String, Any>("howOld" to testActor.age)))
    }
}
