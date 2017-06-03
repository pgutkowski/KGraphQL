package com.github.pgutkowski.kgraphql.integration

import com.github.pgutkowski.kgraphql.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class QueryTest : BaseSchemaTest() {
    @Test
    fun `query nested selection set`(){
        val map = execute("{film{title, director{name, age}}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/film/title"), equalTo(prestige.title))
        assertThat(extract<String>(map, "data/film/director/name"), equalTo(prestige.director.name))
        assertThat(extract<Int>(map, "data/film/director/age"), equalTo(prestige.director.age))
    }

    @Test
    fun `query collection field`(){
        val map = execute("{film{title, director{favActors{name, age}}}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, String>>(map, "data/film/director/favActors[0]"), equalTo(mapOf(
                "name" to prestige.director.favActors[0].name,
                "age" to prestige.director.favActors[0].age)
        ))
    }

    @Test
    fun `query scalar field`(){
        val map = execute("{film{id}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/film/id"), equalTo("${prestige.id.literal}:${prestige.id.numeric}"))
    }

    @Test
    fun `query with selection set on collection`(){
        val map = execute("{film{title, director{favActors{name}}}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, String>>(map, "data/film/director/favActors[0]"), equalTo(mapOf("name" to prestige.director.favActors[0].name)))
    }

    @Test
    fun `query with selection set on collection 2`(){
        val map = execute("{film{title, director{favActors{age}}}}")
        assertNoErrors(map)
        assertThat(extract<Map<String, Int>>(map, "data/film/director/favActors[0]"), equalTo(mapOf("age" to prestige.director.favActors[0].age)))
    }

    @Test
    fun `query with invalid field name`(){
        expect<SyntaxException>("property [favActors] on Director does not exist"){
            execute("{film{title, director{name, [favActors]}}}")
        }
    }

    @Test
    fun `query with argument`(){
        val map = execute("{filmByRank(rank: 1){title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/filmByRank/title"), equalTo("Prestige"))
    }

    @Test
    fun `query with argument 2`(){
        val map = execute("{filmByRank(rank: 2){title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/filmByRank/title"), equalTo("Se7en"))
    }

    @Test
    fun `query with additional argument`(){
        val map = execute("{filmByRank(rank: 2, additional: true){title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/filmByRank/title"), equalTo("Se7en"))
    }

    @Test
    fun `query with alias`(){
        val map = execute("{bestFilm: filmByRank(rank: 1){title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/bestFilm/title"), equalTo("Prestige"))
    }

    @Test
    fun `query with field alias`(){
        val map =execute("{filmByRank(rank: 2){fullTitle: title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/filmByRank/fullTitle"), equalTo("Se7en"))
    }

    @Test
    fun `query with multiple aliases`(){
        val map = execute("{bestFilm: filmByRank(rank: 1){title}, secondBestFilm: filmByRank(rank: 2){title}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/bestFilm/title"), equalTo("Prestige"))
        assertThat(extract<String>(map, "data/secondBestFilm/title"), equalTo("Se7en"))
    }

    @Test
    fun `query with ignored property`(){
        expect<SyntaxException>("property author on Scenario does not exist"){
            execute("{scenario{author, content}}")
        }
    }

    @Test
    fun `invalid query wth duplicated aliases`(){
        expect<SyntaxException>("Duplicated property name/alias: bestFilm"){
            execute("{bestFilm: filmByRank(rank: 1){title}, bestFilm: filmByRank(rank: 2){title}}")
        }
    }

    @Test
    fun `query with interface`(){
        val map = execute("{randomPerson{name \n age}}")
        assertThat(extract<Map<String, String>>(map, "data/randomPerson"), equalTo(mapOf(
                "name" to davidFincher.name,
                "age" to davidFincher.age)
        ))
    }

    @Test
    fun `query with collection elements interface`(){
        val map = execute("{people{name, age}}")
        assertThat(extract<Map<String, String>>(map, "data/people[0]"), equalTo(mapOf(
                "name" to davidFincher.name,
                "age" to davidFincher.age)
        ))
    }

    @Test
    fun `query extension property`(){
        val map = execute("{actors{name, age, isOld}}")
        for(i in 0..4){
            val isOld = extract<Boolean>(map, "data/actors[$i]/isOld")
            val age = extract<Int>(map, "data/actors[$i]/age")
            assertThat(isOld, equalTo(age > 500))
        }
    }

    @Test
    fun `query extension property with arguments`(){
        val map = execute("{actors{name, picture(big: true)}}")
        for(i in 0..4){
            val name = extract<String>(map, "data/actors[$i]/name").replace(' ', '_')
            assertThat(extract<String>(map, "data/actors[$i]/picture"), equalTo("http://picture.server/pic/$name?big=true"))
        }
    }

    @Test
    fun `query extension property with optional argument`(){
        val map = execute("{actors{name, picture}}")
        for(i in 0..4){
            val name = extract<String>(map, "data/actors[$i]/name").replace(' ', '_')
            assertThat(extract<String>(map, "data/actors[$i]/picture"), equalTo("http://picture.server/pic/$name?big=false"))
        }
    }

    @Test
    fun `query with transformed property`(){
        val map = execute("{scenario{id, content(uppercase: false)}}")
        assertThat(extract<String>(map, "data/scenario/content"), equalTo("Very long scenario"))

        val map2 = execute("{scenario{id, content(uppercase: true)}}")
        assertThat(extract<String>(map2, "data/scenario/content"), equalTo("VERY LONG SCENARIO"))
    }

    @Test
    fun `query with invalid field arguments`(){
        expect<ValidationException>("Property id on type Scenario has no arguments, found: [uppercase]"){
            execute("{scenario{id(uppercase: true), content}}")
        }
    }

    @Test
    fun `query union property`(){
        val map = execute("{actors{name, favourite{ ... on Actor {name}, ... on Director {name age}, ... on Scenario{content(uppercase: false)}}}}", null)
        for(i in 0..4){
            val name = extract<String>(map, "data/actors[$i]/name")
            val favourite = extract<Map<String, String>>(map, "data/actors[$i]/favourite")
            when(name){
                "Brad Pitt" -> assertThat(favourite, equalTo(mapOf("name" to "Tom Hardy")))
                "Tom Hardy" -> assertThat(favourite, equalTo(mapOf("age" to 43, "name" to "Christopher Nolan")))
                "Morgan Freeman" -> assertThat(favourite, equalTo(mapOf("content" to "DUMB")))
            }
        }
    }

    @Test
    fun `query union property with external fragment`(){
        val map = execute("{actors{name, favourite{ ...actor, ...director, ...scenario }}}" +
                "fragment actor on Actor {name}" +
                "fragment director on Director {name age}" +
                "fragment scenario on Scenario{content(uppercase: false)} ", null)
        for(i in 0..4){
            val name = extract<String>(map, "data/actors[$i]/name")
            val favourite = extract<Map<String, String>>(map, "data/actors[$i]/favourite")
            when(name){
                "Brad Pitt" -> assertThat(favourite, equalTo(mapOf("name" to "Tom Hardy")))
                "Tom Hardy" -> assertThat(favourite, equalTo(mapOf("age" to 43, "name" to "Christopher Nolan")))
                "Morgan Freeman" -> assertThat(favourite, equalTo(mapOf("content" to "DUMB")))
            }
        }
    }

    @Test
    fun `query union property with invalid selection set`(){
        expect<SyntaxException>("Invalid selection set with properties: [name] on union type property favourite : [Actor, Scenario, Director]"){
            execute("{actors{name, favourite{ name }}}")
        }
    }

    @Test
    fun `query with external fragment`(){
        val map = execute("{film{title, ...dir }} fragment dir {director{name, age}}")
        assertNoErrors(map)
        assertThat(extract<String>(map, "data/film/title"), equalTo(prestige.title))
        assertThat(extract<String>(map, "data/film/director/name"), equalTo(prestige.director.name))
        assertThat(extract<Int>(map, "data/film/director/age"), equalTo(prestige.director.age))
    }

    @Test
    fun `query with missing selection set`(){
        expect<SyntaxException>("Missing selection set on property film of type Film"){
            execute("{film}")
        }
    }

    @Test
    fun `query with inline fragment with type condition`(){
        val map = execute("{people{name, age, ... on Actor {isOld} ... on Director {favActors{name}}}}")
        assertNoErrors(map)
        for(i in extract<List<*>>(map, "data/people").indices){
            val name = extract<String>(map, "data/people[$i]/name")
            when(name){
                "David Fincher" /* director */  ->{
                    assertThat(extract<List<*>>(map, "data/people[$i]/favActors"), notNullValue())
                    assertThat(extractOrNull<Boolean>(map, "data/people[$i]/isOld"), nullValue())
                }
                "Brad Pitt" /* actor */ -> {
                    assertThat(extract<Boolean>(map, "data/people[$i]/isOld"), notNullValue())
                    assertThat(extractOrNull<List<*>>(map, "data/people[$i]/favActors"), nullValue())
                }
            }
        }
    }

    @Test
    fun `query with external fragment with type condition`(){
        val map = execute("{people{name, age ...act ...dir}} fragment act on Actor {isOld} fragment dir on Director {favActors{name}}")
        assertNoErrors(map)
        for(i in extract<List<*>>(map, "data/people").indices){
            val name = extract<String>(map, "data/people[$i]/name")
            when(name){
                "David Fincher" /* director */  ->{
                    assertThat(extract<List<*>>(map, "data/people[$i]/favActors"), notNullValue())
                    assertThat(extractOrNull<Boolean>(map, "data/people[$i]/isOld"), nullValue())
                }
                "Brad Pitt" /* actor */ -> {
                    assertThat(extract<Boolean>(map, "data/people[$i]/isOld"), notNullValue())
                    assertThat(extractOrNull<List<*>>(map, "data/people[$i]/favActors"), nullValue())
                }
            }
        }
    }
}