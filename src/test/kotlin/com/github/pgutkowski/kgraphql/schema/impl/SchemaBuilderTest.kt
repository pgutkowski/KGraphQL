package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.Id
import com.github.pgutkowski.kgraphql.Scenario
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import java.util.*


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
    }

    @Test
    fun testTransformation() {
        val tested = defaultSchema {
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
    }

    @Test
    fun testExtensionProperty(){
        val tested = defaultSchema {

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
    }

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
    }
}