package com.github.pgutkowski.kgraphql.integration

import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class QueryTest : BaseSchemaTest() {
    @Test
    fun testBasicJsonQuery(){
        val map = execute("{film{title, director{name, age}}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/film/title"), equalTo(prestige.title))
        assertThat(extract<String>(map, "data/film/director/name"), equalTo(prestige.director.name))
        assertThat(extract<Int>(map, "data/film/director/age"), equalTo(prestige.director.age))
    }

    @Test
    fun testCollections(){
        val map = execute("{film{title, director{favActors{name, age}}}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, String>>(map, "data/film/director/favActors[0]"), equalTo(mapOf(
                "name" to prestige.director.favActors[0].name,
                "age" to prestige.director.favActors[0].age)
        ))
    }

    @Test
    fun testScalar(){
        val map = execute("{film{id}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/film/id"), equalTo("${prestige.id.literal}:${prestige.id.numeric}"))
    }

    @Test
    fun testCollectionEntriesProperties(){
        val map = execute("{film{title, director{favActors{name}}}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, String>>(map, "data/film/director/favActors[0]"), equalTo(mapOf("name" to prestige.director.favActors[0].name)))
    }

    @Test
    fun testCollectionEntriesProperties2(){
        val map = execute("{film{title, director{favActors{age}}}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, Int>>(map, "data/film/director/favActors[0]"), equalTo(mapOf("age" to prestige.director.favActors[0].age)))
    }

    @Test
    fun testInvalidPropertyName(){
        val map = execute("{film{title, director{name, [favActors]}}}")
        assertError(map, "property [favActors] on Director does not exist")
    }

    @Test
    fun testQueryWithArgument(){
        val map = execute("{filmByRank(rank: 1){title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/filmByRank/title"), equalTo("Prestige"))
    }

    @Test
    fun testQueryWithArgument2(){
        val map = execute("{filmByRank(rank: 2){title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/filmByRank/title"), equalTo("Se7en"))
    }

    @Test
    fun testQueryWithAlias(){
        val map = execute("{bestFilm: filmByRank(rank: 1){title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/bestFilm/title"), equalTo("Prestige"))
    }

    @Test
    fun testQueryWithFieldAlias(){
        val map =execute("{filmByRank(rank: 2){fullTitle: title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/filmByRank/fullTitle"), equalTo("Se7en"))
    }

    @Test
    fun testQueryWithAliases(){
        val map = execute("{bestFilm: filmByRank(rank: 1){title}, secondBestFilm: filmByRank(rank: 2){title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/bestFilm/title"), equalTo("Prestige"))
        assertThat(extract<String>(map, "data/secondBestFilm/title"), equalTo("Se7en"))
    }

    @Test
    fun testQueryWithIgnoredProperty(){
        val map = execute("{scenario{author, content}}")
        assertError(map, "SyntaxException: property author on Scenario does not exist")
    }

    @Test
    fun testInvalidQueryWithDuplicatedAliases(){
        val map = execute("{bestFilm: filmByRank(rank: 1){title}, bestFilm: filmByRank(rank: 2){title}}")
        assertError(map, "SyntaxException: Duplicated property name/alias: bestFilm")
    }

    @Test
    fun testCastToSuperclass(){
        val map = execute("{randomPerson{name \n age}}")
        assertThat(extract<Map<String, String>>(map, "data/randomPerson"), equalTo(mapOf(
                "name" to davidFincher.name,
                "age" to davidFincher.age)
        ))
    }

    @Test
    fun testCastListElementsToSuperclass(){
        val map = execute("{people{name, age}}")
        assertThat(extract<Map<String, String>>(map, "data/people[0]"), equalTo(mapOf(
                "name" to davidFincher.name,
                "age" to davidFincher.age)
        ))
    }

    @Test
    fun testExtensionProperty(){
        val map = execute("{actors{name, age, isOld}}")
        for(i in 0..4){
            val isOld = extract<Boolean>(map, "data/actors[$i]/isOld")
            val age = extract<Int>(map, "data/actors[$i]/age")
            assertThat(isOld, equalTo(age > 500))
        }
    }

    @Test
    fun testExtensionPropertyWithArgument(){
        val map = execute("{actors{name, picture(big: true)}}")
        for(i in 0..4){
            val name = extract<String>(map, "data/actors[$i]/name").replace(' ', '_')
            assertThat(extract<String>(map, "data/actors[$i]/picture"), equalTo("http://picture.server/pic/$name?big=true"))
        }
    }

    @Test
    fun testExtensionPropertyWithOptionalArgument(){
        val map = execute("{actors{name, picture}}")
        for(i in 0..4){
            val name = extract<String>(map, "data/actors[$i]/name").replace(' ', '_')
            assertThat(extract<String>(map, "data/actors[$i]/picture"), equalTo("http://picture.server/pic/$name?big=false"))
        }
    }

    @Test
    fun testPropertyTransformation(){
        val map = execute("{scenario{id, content(uppercase: false)}}")
        assertThat(extract<String>(map, "data/scenario/content"), equalTo("Very long scenario"))

        val map2 = execute("{scenario{id, content(uppercase: true)}}")
        assertThat(extract<String>(map2, "data/scenario/content"), equalTo("VERY LONG SCENARIO"))
    }

    @Test
    fun testInvalidPropertyArguments(){
        val map = execute("{scenario{id(uppercase: true), content}}")
        assertError(map, "ValidationException: Property id on type Scenario has no arguments, found: [uppercase]")
    }
}