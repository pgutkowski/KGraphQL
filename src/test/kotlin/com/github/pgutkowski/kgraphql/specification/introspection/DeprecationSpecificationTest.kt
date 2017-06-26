package com.github.pgutkowski.kgraphql.specification.introspection

import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class DeprecationSpecificationTest {

    @Test
    fun `queries may be documented`(){
        val expected = "sample query"
        val schema = defaultSchema {
            query("sample"){
                deprecate(expected)
                resolver<String> {"SAMPLE"}
            }
        }

        val response = deserialize(schema.execute("{__schema{queryType{fields{name, deprecationReason, isDeprecated}}}}"))
        assertThat(response.extract("data/__schema/queryType/fields[0]/deprecationReason"), equalTo(expected))
        assertThat(response.extract("data/__schema/queryType/fields[0]/isDeprecated"), equalTo(true))
    }

    @Test
    fun `mutations may be documented`(){
        val expected = "sample mutation"
        val schema = defaultSchema {
            mutation("sample"){
                deprecate(expected)
                resolver<String> {"SAMPLE"}
            }
        }

        val response = deserialize(schema.execute("{__schema{mutationType{fields{name, deprecationReason, isDeprecated}}}}"))
        assertThat(response.extract("data/__schema/mutationType/fields[0]/deprecationReason"), equalTo(expected))
        assertThat(response.extract("data/__schema/mutationType/fields[0]/isDeprecated"), equalTo(true))
    }

    data class Sample(val content : String)

    @Test
    fun `kotlin field may be documented`(){
        val expected = "sample type"
        val schema = defaultSchema {
            query("sample"){
                resolver<String> {"SAMPLE"}
            }

            type<Sample>{
                Sample::content.configure {
                    deprecate(expected)
                }
            }
        }

        val response = deserialize(schema.execute("{__type(name: \"Sample\"){fields{isDeprecated, deprecationReason}}}"))
        assertThat(response.extract("data/__type/fields[0]/deprecationReason/"), equalTo(expected))
        assertThat(response.extract("data/__type/fields[0]/isDeprecated/"), equalTo(true))
    }

    @Test
    fun `extension field may be documented`(){
        val expected = "sample type"
        val schema = defaultSchema {
            query("sample"){
                resolver<String> {"SAMPLE"}
            }

            type<Sample>{
                property<String>("add"){
                    deprecate(expected)
                    resolver{ (content) -> content.toUpperCase() }
                }
            }
        }

        val response = deserialize(schema.execute("{__type(name: \"Sample\"){fields{name, isDeprecated, deprecationReason}}}"))
        assertThat(response.extract("data/__type/fields[1]/deprecationReason"), equalTo(expected))
        assertThat(response.extract("data/__type/fields[1]/isDeprecated"), equalTo(true))
    }

    enum class SampleEnum { ONE, TWO, THREE }

    @Test
    fun `enum value may be deprecated`(){
        val expected = "some enum value"
        val schema = defaultSchema {
            query("sample"){
                resolver<String> {"SAMPLE"}
            }

            enum<SampleEnum> {
                value(SampleEnum.ONE){
                    deprecate(expected)
                }
            }
        }

        val response = deserialize(schema.execute("{__type(name: \"SampleEnum\"){enumValues{name, isDeprecated, deprecationReason}}}"))
        assertThat(response.extract("data/__type/enumValues[0]/name"), equalTo(SampleEnum.ONE.name))
        assertThat(response.extract("data/__type/enumValues[0]/deprecationReason"), equalTo(expected))
        assertThat(response.extract("data/__type/enumValues[0]/isDeprecated"), equalTo(true))
    }
}