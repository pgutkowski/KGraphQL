package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.TestClasses
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.extract
import com.github.pgutkowski.kgraphql.schema.SchemaBuilder
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.AssumptionViolatedException
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

    val testFilm2 = TestClasses.Film(
            TestClasses.Id("Se7en", 1995),
            1995, "Se7en",
            TestClasses.Director(
                    "David Fincher", 43,
                    listOf (
                            TestClasses.Actor("Brad Pitt", 763),
                            TestClasses.Actor("Morgan Freeman", 1212),
                            TestClasses.Actor("Kevin Spacey", 2132)
                    )
            )
    )

    val actors = mutableListOf<TestClasses.Actor>()

    val testedSchema = SchemaBuilder()
            .query( "film", { -> testFilm } )
            .query("filmByRank", { rank: Int -> when(rank){
                1 -> testFilm
                2 -> testFilm2
                else -> null
            }})
            .query("filmsByType", {type: TestClasses.FilmType -> listOf(testFilm, testFilm2) })

            .mutation("createActor", { name : String, age : Int ->
                val actor = TestClasses.Actor(name, age)
                actors.add(actor)
                actor
            })
            .scalar(TestClasses.IdScalarSupport())
            .enum<TestClasses.FilmType>()
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

    fun execute(query: String, variables : String? = null) = deserialize(testedSchema.handleRequest(query, variables))
}