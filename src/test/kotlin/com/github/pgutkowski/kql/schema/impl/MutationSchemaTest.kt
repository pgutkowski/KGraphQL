package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.TestClasses
import com.github.pgutkowski.kql.deserialize
import com.github.pgutkowski.kql.extract
import junit.framework.Assert
import junit.framework.Assert.assertEquals
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class MutationSchemaTest : BaseSchemaTest() {


    val testActor = TestClasses.Actor("Michael Caine", 72)

    @Test
    fun testSimpleMutation(){
        val result = testedSchema.handleRequest(
                "mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age})}"
        )
        val map = deserialize(result)
        assertNoErrors(map)
        assertThat(extract<Map<String, Any>>(map, "data/createActor"), equalTo(mapOf("name" to testActor.name, "age" to testActor.age)))
    }

    @Test
    fun testSimpleMutationWithFields(){
        val result = testedSchema.handleRequest(
                "mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){name}}"
        )
        val map = deserialize(result)
        assertNoErrors(map)
        assertThat(extract<Map<String, Any>>(map, "data/createActor"), equalTo(mapOf<String, Any>("name" to testActor.name)))
    }

    @Test
    fun testSimpleMutationWithFields2(){
        val result = testedSchema.handleRequest(
                "mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}){age}}"
        )
        val map = deserialize(result)
        assertNoErrors(map)
        assertThat(extract<Map<String, Any>>(map, "data/createActor"), equalTo(mapOf<String, Any>("age" to testActor.age)))
    }

    @Test
    fun testInvalidMutationName(){
        val result = testedSchema.handleRequest(
                "mutation {createBanana(name: \"${testActor.name}\", age: ${testActor.age}){age}}"
        )
        val map = deserialize(result)
        assertError(map, "SyntaxException: Mutation: createBanana is not supported by this schema")
    }

    @Test
    fun testInvalidArgumentType(){
        val result = testedSchema.handleRequest(
                "mutation {createActor(name: \"${testActor.name}\", age: \"fwfwf\"){age}}"
        )
        val map = deserialize(result)
        assertError(map, "SyntaxException: argument 'fwfwf' is not value of type: class kotlin.Int")
    }

    @Test
    fun testInvalidArgumentNumber(){
        val result = testedSchema.handleRequest(
                "mutation {createActor(name: \"${testActor.name}\", age: ${testActor.age}, bananan: \"fwfwf\"){age}}"
        )
        val map = deserialize(result)
        assertError(map, "SyntaxException: Mutation function createActor with arguments: [name, bananan, age] not found")
    }
}