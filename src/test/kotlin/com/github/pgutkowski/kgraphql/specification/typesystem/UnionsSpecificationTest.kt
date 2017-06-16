package com.github.pgutkowski.kgraphql.specification.typesystem

import com.github.pgutkowski.kgraphql.*
import com.github.pgutkowski.kgraphql.integration.BaseSchemaTest
import com.github.pgutkowski.kgraphql.schema.SchemaException
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test


@Specification("3.1.4 Unions")
class UnionsSpecificationTest : BaseSchemaTest() {

    @Test
    fun `query union property`(){
        val map = execute("{actors{name, favourite{ ... on Actor {name}, ... on Director {name age}, ... on Scenario{content(uppercase: false)}}}}", null)
        for(i in 0..4){
            val name = extract<String>(map, "data/actors[$i]/name")
            val favourite = extract<Map<String, String>>(map, "data/actors[$i]/favourite")
            when(name){
                "Brad Pitt" -> MatcherAssert.assertThat(favourite, CoreMatchers.equalTo(mapOf("name" to "Tom Hardy")))
                "Tom Hardy" -> MatcherAssert.assertThat(favourite, CoreMatchers.equalTo(mapOf("age" to 43, "name" to "Christopher Nolan")))
                "Morgan Freeman" -> MatcherAssert.assertThat(favourite, CoreMatchers.equalTo(mapOf("content" to "DUMB")))
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
                "Brad Pitt" -> MatcherAssert.assertThat(favourite, CoreMatchers.equalTo(mapOf("name" to "Tom Hardy")))
                "Tom Hardy" -> MatcherAssert.assertThat(favourite, CoreMatchers.equalTo(mapOf("age" to 43, "name" to "Christopher Nolan")))
                "Morgan Freeman" -> MatcherAssert.assertThat(favourite, CoreMatchers.equalTo(mapOf("content" to "DUMB")))
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
    fun `The member types of a Union type must all be Object base types`(){
        expect<SchemaException>("The member types of a Union type must all be Object base types"){
            KGraphQL.schema {
                unionType("invalid") {
                    type<String>()
                }
            }
        }
    }

    @Test
    fun `A Union type must define one or more unique member types`(){
        expect<SchemaException>("A Union type must define one or more unique member types"){
            KGraphQL.schema {
                unionType("invalid") {}
            }
        }
    }

    /**
     * Kotlin is non-nullable by default (T!), so test covers only case for collections
     */
    @Test
    fun `List may not be member type of a Union`(){
        expect<SchemaException>("Collection may not be member type of a Union 'Invalid'"){
            KGraphQL.schema {
                unionType("Invalid") {
                    type<Collection<*>>()
                }
            }
        }

        expect<SchemaException>("Map may not be member type of a Union 'Invalid'"){
            KGraphQL.schema {
                unionType("Invalid") {
                    type<Map<String, String>>()
                }
            }
        }
    }

    @Test
    fun `Function type may not be member types of a Union`(){
        expect<SchemaException>("Cannot handle function class kotlin.Function as Object type"){
            KGraphQL.schema {
                unionType("invalid") {
                    type<Function<*>>()
                }
            }
        }
    }
}