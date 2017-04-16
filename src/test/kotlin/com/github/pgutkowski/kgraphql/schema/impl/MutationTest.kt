package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.TestClasses
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class MutationTest : BaseSchemaTest() {

    val testActor = TestClasses.Actor("Michael Caine", 72)

    @Test
    fun testSimpleMutation(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age})}")
        assertNoErrors(map)
        assertThat(extract<Map<String, Any>>(map, "data/createActor"), equalTo(mapOf("name" to testActor.name, "age" to testActor.age)))
    }

    @Test
    fun testSimpleMutationWithFields(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){name}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, Any>>(map, "data/createActor"), equalTo(mapOf<String, Any>("name" to testActor.name)))
    }

    @Test
    fun testSimpleMutationWithFields2(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){age}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, Any>>(map, "data/createActor"), equalTo(mapOf<String, Any>("age" to testActor.age)))
    }

    @Test
    fun testInvalidMutationName(){
        val map = execute("mutation {createBanana(name: \"${testActor.name}\", age: ${testActor.age}){age}}")
        assertError(map, "SyntaxException: Mutation: createBanana is not supported by this schema")
    }

    @Test
    fun testInvalidArgumentType(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: \"fwfwf\"){age}}")
        assertError(map, "SyntaxException: argument 'fwfwf' is not value of type: kotlin.Int")
    }

    @Test
    fun testInvalidArgumentNumber(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}, bananan: \"fwfwf\"){age}}")
        assertError(map, "SyntaxException: Mutation createActor does support arguments: [name, age]. found arguments: [name, bananan, age]")
    }

    @Test
    fun testSimpleMutationWithAlias(){
        val map = execute("{caine : createActor(name: \"${testActor.name}\", age: ${testActor.age}){age}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, Any>>(map, "data/caine"), equalTo(mapOf<String, Any>("age" to testActor.age)))
    }

    @Test
    fun testSimpleMutationWithFieldAlias(){
        val map = execute("mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){howOld: age}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, Any>>(map, "data/createActor"), equalTo(mapOf<String, Any>("howOld" to testActor.age)))
    }
}
