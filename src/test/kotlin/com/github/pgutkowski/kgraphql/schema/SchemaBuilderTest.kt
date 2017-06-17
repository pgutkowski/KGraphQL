package com.github.pgutkowski.kgraphql.schema

import com.github.pgutkowski.kgraphql.Actor
import com.github.pgutkowski.kgraphql.FilmType
import com.github.pgutkowski.kgraphql.Id
import com.github.pgutkowski.kgraphql.KGraphQL
import com.github.pgutkowski.kgraphql.Scenario
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.scalar.StringScalarCoercion
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.*
import kotlin.reflect.full.starProjectedType

/**
 * Tests for SchemaBuilder behaviour, not request execution
 */
class SchemaBuilderTest {
    @Test
    fun `DSL created UUID scalar support`(){

        val testedSchema = defaultSchema {
            stringScalar<UUID> {
                description = "unique identifier of object"
                deserialize = { uuid : String -> UUID.fromString(uuid) }
                serialize = UUID::toString
            }
        }

        val uuidScalar = testedSchema.model.scalars.find { it.name == "UUID" }!!.coercion as StringScalarCoercion<UUID>
        val testUuid = UUID.randomUUID()
        MatcherAssert.assertThat(uuidScalar.serialize(testUuid), CoreMatchers.equalTo(testUuid.toString()))
        MatcherAssert.assertThat(uuidScalar.deserialize(testUuid.toString()), CoreMatchers.equalTo(testUuid))
    }

    @Test
    fun `ignored property DSL`() {
        val testedSchema = defaultSchema {
            query("scenario") {
                resolver { -> Scenario(Id("GKalus", 234234), "Gamil Kalus", "TOO LONG") }
            }
            type<Scenario>{
                ignore(Scenario::author)
            }
        }

        val scenarioType = testedSchema.structure.nodes[Scenario::class.starProjectedType]
                ?: throw Exception("Scenario type should be present in schema")
        assertThat(scenarioType.properties["author"], nullValue())
        assertThat(scenarioType.properties["content"], notNullValue())
    }

    @Test
    fun `transformation DSL`() {
        val testedSchema = defaultSchema {
            query("scenario") {
                resolver { -> Scenario(Id("GKalus", 234234),"Gamil Kalus", "TOO LONG") }
            }
            type<Scenario> {

                transformation(Scenario::content, { content: String, capitalized : Boolean? ->
                    if(capitalized == true) content.capitalize() else content
                })
            }
        }
        val scenarioType = testedSchema.structure.nodes[Scenario::class.starProjectedType]
                ?: throw Exception("Scenario type should be present in schema")
        assert(scenarioType.kqlType is KQLType.Object<*>)
        val kqlType = scenarioType.kqlType as KQLType.Object<*>
        assertThat(kqlType.transformations.getOrNull(0), notNullValue())
    }

    @Test
    fun `extension property DSL`(){
        val testedSchema = defaultSchema {

            query("scenario") {
                resolver { -> Scenario(Id("GKalus", 234234),"Gamil Kalus", "TOO LONG") }
            }

            type<Scenario> {
                property<String>("pdf") {
                    description = "link to pdf representation of scenario"
                    resolver { scenario : Scenario -> "http://scenarios/${scenario.id}" }
                }
            }
        }

        val scenarioType = testedSchema.structure.nodes[Scenario::class.starProjectedType]
                ?: throw Exception("Scenario type should be present in schema")
        assert(scenarioType.kqlType is KQLType.Object<*>)
        val kqlType = scenarioType.kqlType as KQLType.Object<*>

        assertThat(kqlType.extensionProperties.getOrNull(0), notNullValue())
        assertThat(scenarioType.properties.keys, hasItem("pdf"))
    }

    @Test
    fun `union type DSL`(){
        val tested = defaultSchema {

            query("scenario") {
                resolver { -> Scenario(Id("GKalus", 234234),"Gamil Kalus", "TOO LONG") }
            }

            val linked = unionType("Linked") {
                type<Actor>()
                type<Scenario>()
            }

            type<Scenario> {
                unionProperty("pdf") {
                    returnType = linked
                    description = "link to pdf representation of scenario"
                    resolver { scenario : Scenario ->
                        if(scenario.author.startsWith("Gamil")){
                            Scenario(Id("ADD", 22), "gambino", "nope")
                        } else{
                            Actor("Chance", 333)
                        }
                    }
                }
            }
        }

        val scenarioType = tested.structure.nodes[Scenario::class.starProjectedType]
                ?: throw Exception("Scenario type should be present in schema")
        assert(scenarioType.kqlType is KQLType.Object<*>)
        val unionProperty = scenarioType.unionProperties["pdf"] ?: throw Exception("Scenario should have union property 'pdf'")
        assertThat(unionProperty, notNullValue())
    }

    @Test
    fun `circular dependency extension property`(){
        val tested = defaultSchema {
            query("actor") {
                resolver { -> Actor("Little John", 44) }
            }

            type<Actor> {
                property<Actor>("linked") {
                    resolver { _ -> Actor("BIG John", 3234) }
                }
            }
        }

        val actorType = tested.structure.nodes[Actor::class.starProjectedType]
                ?: throw Exception("Scenario type should be present in schema")
        assert(actorType.kqlType is KQLType.Object<*>)
        val property = actorType.properties["linked"] ?: throw Exception("Actor should have ext property 'linked'")
        assertThat(property, notNullValue())
        assertThat(property.returnType.kqlType.name, equalTo("Actor"))
    }

    @Test
    fun ` _ is allowed as receiver argument name`(){
        val schema = defaultSchema {
            query("actor") {
                resolver { -> Actor("Boguś Linda", 4343) }
            }

            type<Actor>{
                property<List<String>>("favDishes") {
                    resolver { _: Actor, size: Int->
                        listOf("steak", "burger", "soup", "salad", "bread", "bird").take(size)
                    }
                }
            }
        }

        deserialize(schema.execute("{actor{favDishes(size: 2)}}"))
    }

    @Test
    fun `Custom type name`(){
        val schema = defaultSchema {
            query("actor") {
                resolver { type: FilmType -> Actor("Boguś Linda $type", 4343)  }
            }

            enum<FilmType> {
                name = "TYPE"
            }
        }

        val result = deserialize(schema.execute("query(\$type : TYPE = FULL_LENGTH){actor(type: \$type){name}}"))
        assertThat(extract<String>(result, "data/actor/name"), equalTo("Boguś Linda FULL_LENGTH"))
    }

    private data class LambdaWrapper(val lambda : () -> Int)

    @Test
    fun `function properties cannot be handled`(){
        expect<SchemaException>("Cannot handle function class kotlin.Function0 as Object type"){
            KGraphQL.schema {
                query("lambda"){
                    resolver { -> LambdaWrapper({ 1 }) }
                }
            }
        }
    }
}