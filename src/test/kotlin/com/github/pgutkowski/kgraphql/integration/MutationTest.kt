package com.github.pgutkowski.kgraphql.integration

import com.github.pgutkowski.kgraphql.Actor
import com.github.pgutkowski.kgraphql.assertError
import com.github.pgutkowski.kgraphql.assertNoErrors
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test


class MutationTest : BaseSchemaTest() {

    val testActor = Actor("Michael Caine", 72)

    @Test
    fun `simple mutation multiple fields`(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){name, age}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, Any>>(map, "data/createActor"), equalTo(mapOf("name" to testActor.name, "age" to testActor.age)))
    }

    @Test
    fun `simple mutation single field`(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){name}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, Any>>(map, "data/createActor"), equalTo(mapOf<String, Any>("name" to testActor.name)))
    }

    @Test
    fun `simple mutation single field 2`(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){age}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, Any>>(map, "data/createActor"), equalTo(mapOf<String, Any>("age" to testActor.age)))
    }

    @Test
    fun `invalid mutation name`(){
        val map = execute("mutation {createBanana(name: \"${testActor.name}\", age: ${testActor.age}){age}}")
        assertError(map, "SyntaxException: createBanana is not supported by this schema")
    }

    @Test
    fun `invalid argument type`(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: \"fwfwf\"){age}}")
        assertError(map, "SyntaxException: argument 'fwfwf' is not value of type Int")
    }

    @Test
    fun `invalid arguments number`(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}, bananan: \"fwfwf\"){age}}")
        assertError(map, "createActor does support arguments: [name, age]. found arguments: [name, bananan, age]")
    }

    @Test
    fun `mutation with alias`(){
        val map = execute("{caine : createActor(name: \"${testActor.name}\", age: ${testActor.age}){age}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, Any>>(map, "data/caine"), equalTo(mapOf<String, Any>("age" to testActor.age)))
    }

    @Test
    fun `mutation with field alias`(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){howOld: age}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, Any>>(map, "data/createActor"), equalTo(mapOf<String, Any>("howOld" to testActor.age)))
    }
}
