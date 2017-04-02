package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.TestClasses
import com.github.pgutkowski.kql.annotation.method.ResolvingFunction
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
                @ResolvingFunction fun getQueryClass() : TestClasses.Film = testFilm
            }))
            .addMutations(object : MutationResolver{
                @ResolvingFunction
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
        MatcherAssert.assertThat(map["data"], CoreMatchers.notNullValue())
        MatcherAssert.assertThat(map["errors"], CoreMatchers.nullValue())
    }
}