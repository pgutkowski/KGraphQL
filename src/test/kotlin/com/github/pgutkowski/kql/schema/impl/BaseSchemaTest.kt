package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.TestClasses
import com.github.pgutkowski.kql.annotation.method.Mutation
import com.github.pgutkowski.kql.annotation.method.Query
import com.github.pgutkowski.kql.extract
import com.github.pgutkowski.kql.resolve.MutationResolver
import com.github.pgutkowski.kql.resolve.QueryResolver
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
            .addInput(TestClasses.InputClass::class)
            .addQueryField(TestClasses.Film::class, listOf(object: QueryResolver<TestClasses.Film> {
                @Query fun getQueryClass() : TestClasses.Film = testFilm
            }))
            .addMutations(object : MutationResolver{
                @Mutation
                fun createActor(name: String, age: Int) : TestClasses.Actor{
                    val actor = TestClasses.Actor(name, age)
                    actors.add(actor)
                    return actor
                }
            })
            .addScalar(TestClasses.Id::class, TestClasses.IdScalarSupport())
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