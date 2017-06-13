package com.github.pgutkowski.kgraphql.schema

import com.github.pgutkowski.kgraphql.*
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.reflect.full.starProjectedType

/**
 * Tests for SchemaBuilder behaviour, not request execution
 */
class SchemaBuilderTest {
    @Test
    fun `DSL created UUID scalar support`(){

        val testedSchema = defaultSchema {
            scalar<UUID> {
                description = "unique identifier of object"
                serialize = { uuid : String -> UUID.fromString(uuid) }
                deserialize = UUID::toString
                validate = String::isNotBlank
            }
        }

        val uuidScalar = testedSchema.model.scalars.find { it.name == "UUID" }!!.scalarSupport as ScalarSupport<UUID>
        val testUuid = UUID.randomUUID()
        MatcherAssert.assertThat(uuidScalar.deserialize(testUuid), CoreMatchers.equalTo(testUuid.toString()))
        MatcherAssert.assertThat(uuidScalar.serialize(testUuid.toString()), CoreMatchers.equalTo(testUuid))
        MatcherAssert.assertThat(uuidScalar.validate(testUuid.toString()), CoreMatchers.equalTo(true))
    }

    @Test
    fun `ignored property DSL`() {
        val testedSchema = defaultSchema {
            query {
                name = "scenario"
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
            query {
                name = "scenario"
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

            query {
                name = "scenario"
                resolver { -> Scenario(Id("GKalus", 234234),"Gamil Kalus", "TOO LONG") }
            }

            type<Scenario> {
                property<String> {
                    name = "pdf"
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

            query {
                name = "scenario"
                resolver { -> Scenario(Id("GKalus", 234234),"Gamil Kalus", "TOO LONG") }
            }

            unionType {
                name = "Linked"
                type<Actor>()
                type<Scenario>()
            }

            type<Scenario> {
                unionProperty {
                    name = "pdf"
                    returnType = "Linked"
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
            query {
                name = "actor"
                resolver { -> Actor("Little John", 44) }
            }

            type<Actor> {
                property<Actor> {
                    name = "linked"
                    resolver { actor -> Actor("BIG John", 3234) }
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
            query {
                name = "actor"
                resolver { -> Actor("Boguś Linda", 4343) }
            }

            type<Actor>{
                property<List<String>> {
                    name = "favDishes"
                    resolver { _: Actor, size: Int->
                        listOf("steak", "burger", "soup", "salad", "bread", "bird").take(size)
                    }
                }
            }
        }

        //just see if it works
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
}