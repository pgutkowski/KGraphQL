package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.TestClasses
import com.github.pgutkowski.kql.extract
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before


abstract class BaseSchemaTest {
    val testFilm = TestClasses.Film(
            TestClasses.Id("Prestige", 2006),
            2006, "Prestige",
            TestClasses.Director(
                    "Christopher Nolan", 43,
                    listOf (
                            TestClasses.Actor("Tom Hardy", 232),
                            TestClasses.Actor("Christian Bale", 232)
                    )
            )
    )

    val actors = mutableListOf<TestClasses.Actor>()

    val testedSchema = DefaultSchemaBuilder()
            .input(TestClasses.InputClass::class)
            .query( "film", { -> testFilm } )
            .mutation("createActor", { name : String, age : Int ->
                val actor = TestClasses.Actor(name, age)
                actors.add(actor)
                actor
            })
            .scalar(TestClasses.Id::class, TestClasses.IdScalarSupport())
            .build() as DefaultSchema

    @Before
    fun cleanup() = actors.clear()

    fun assertNoErrors(map : Map<*,*>) {
        if(map["errors"] != null) throw AssertionError("Errors encountered: ${map["errors"]}")
        if(map["data"] == null) throw AssertionError("Data is null")
    }

    fun assertError(map : Map<*,*>, vararg messageElements : String) {
        val errorMessage = extract<String>(map, "errors/message")
        MatcherAssert.assertThat(errorMessage, CoreMatchers.notNullValue())

        messageElements
                .filterNot { errorMessage.contains(it) }
                .forEach { throw AssertionError("Expected error message to contain $it, but was: $errorMessage") }
    }
}