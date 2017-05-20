package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.Id
import com.github.pgutkowski.kgraphql.Scenario
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.server.asHTML
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.*
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
    fun testDSLCreatedUUIDScalarSupport(){

        val testedSchema = defaultSchema {
            scalar<UUID> {
                description = "unique identifier of object"
                serialize = { uuid : String -> UUID.fromString(uuid) }
                deserialize = UUID::toString
                validate = String::isNotBlank
            }
        }

        val uuidScalar = testedSchema.scalars.find { it.name == "UUID" }!!.scalarSupport as ScalarSupport<UUID>
        val testUuid = UUID.randomUUID()
        MatcherAssert.assertThat(uuidScalar.deserialize(testUuid), CoreMatchers.equalTo(testUuid.toString()))
        MatcherAssert.assertThat(uuidScalar.serialize(testUuid.toString()), CoreMatchers.equalTo(testUuid))
        MatcherAssert.assertThat(uuidScalar.validate(testUuid.toString()), CoreMatchers.equalTo(true))
    }

    @Test
    fun testIgnoredProperty() {
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
    fun testTransformation() {
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
    fun testExtensionProperty(){
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
    fun testUnionType(){
        val tested = defaultSchema {

            query {
                name = "scenario"
                resolver { -> Scenario(Id("GKalus", 234234),"Gamil Kalus", "TOO LONG") }
            }

            unionType {
                name = "Owner"
                type<String>()
                type<Int>()
            }

            type<Scenario> {
                unionProperty {
                    name = "pdf"
                    description = "link to pdf representation of scenario"
                    resolver { scenario : Scenario ->
                        if(scenario.author.startsWith("Gamil")) "http://scenarios/${scenario.id}" else 543
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
}