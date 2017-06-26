package com.github.pgutkowski.kgraphql.specification.introspection

import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.introspection.__List
import com.github.pgutkowski.kgraphql.schema.introspection.__NonNull
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class IntrospectionSpecificationTest {

    @Test
    fun `simple introspection`(){
        val schema : Schema = defaultSchema {
            query("sample"){
                resolver { -> "Ronaldinho" }
            }
        }

        assertThat(schema.findTypeByName("String"), notNullValue())
    }

    data class Data(val string : String)

    @Test
    fun `__typename field can be used to obtain type of object`(){
        val schema : Schema = defaultSchema {
            query("sample"){
                resolver { -> Data("Ronaldingo")}
            }
        }

        val response = deserialize(schema.execute("{sample{string, __typename}}"))
        assertThat(response.extract("data/sample/__typename"), equalTo("Data"))
    }

    @Test
    fun `__typename field cannot be used on scalars`(){
        val schema : Schema = defaultSchema {
            query("sample"){
                resolver { -> Data("Ronaldingo")}
            }
        }

        expect<RequestException>("property __typename on String does not exist"){
            schema.execute("{sample{string{__typename}}}")
        }
    }

    data class Union1(val one : String)

    data class Union2(val two : String)

    @Test
    fun `__typaname field can be used to obtain type of union member in runtime`(){
        val schema = defaultSchema {
            type<Data>{
                unionProperty("union"){
                    returnType = unionType("UNION"){
                        type<Union1>()
                        type<Union2>()
                    }

                    resolver { (string) -> if(string.isEmpty()) Union1("!!") else Union2("??") }
                }
            }

            query("data"){
                resolver { input: String -> Data(input) }
            }
        }

        val response = deserialize(schema.execute(
                "{data(input: \"\"){" +
                        "string, " +
                        "union{" +
                            "... on Union1{one, __typename} " +
                            "... on Union2{two}" +
                        "}" +
                "}}"))

        assertThat(response.extract("data/data/union/__typename"), equalTo("Union1"))
    }

    @Test
    fun `list and nonnull types are wrapping regular types in introspection system`(){
        val schema = defaultSchema {
            query("data"){
                resolver{ -> listOf("BABA") }
            }
        }

        val dataReturnType = schema.queryType.fields?.first()?.type!!
        assert(dataReturnType is __NonNull)
        assert(dataReturnType.ofType is __List)
        assert(dataReturnType.ofType?.ofType is __NonNull)
    }

    @Test
    fun `field __schema is accessible from the type of the root of a query operation`(){
        val schema = defaultSchema {
            query("data"){
                resolver<String>{ "DADA" }
            }
        }

        val response = deserialize(schema.execute("{__schema{queryType{fields{name}}}}"))
        assertThat(response.extract("data/__schema/queryType/fields[0]/name"), equalTo("data"))
    }

    @Test
    fun `field __types is accessible from the type of the root of a query operation`(){
        val schema = defaultSchema {
            query("data"){
                resolver<String>{ "DADA" }
            }
        }

        val response = deserialize(schema.execute("{__type(name: \"String\"){kind, name, description}}"))
        assertThat(response.extract("data/__type/name"), equalTo("String"))
        assertThat(response.extract("data/__type/kind"), equalTo("SCALAR"))
    }

    data class IntString(val int : Int, val string: String)

    @Test
    fun `operation args are introspected`(){
        val schema = defaultSchema {
            query("data"){
                resolver { int: Int, string: String -> IntString(int, string) }
            }
        }

        val inputValues = schema.queryType.fields?.first()?.args
                ?: throw AssertionError("Expected non null field")

        assertThat(inputValues[0].name, equalTo("int"))
        assertThat(inputValues[0].type.ofType?.name, equalTo("Int"))
        assertThat(inputValues[1].name, equalTo("string"))
        assertThat(inputValues[1].type.ofType?.name, equalTo("String"))
    }

    @Test
    fun `fields args are introspected`(){
        val schema = defaultSchema {
            query("data"){
                resolver { int: Int, string: String -> IntString(int, string) }
            }

            type<IntString>{
                property<Double>("float"){
                    resolver { (int), doubleIt : Boolean -> int.toDouble() * if(doubleIt) 2 else 1 }
                }
            }
        }

        val inputValues = schema.findTypeByName("IntString")?.fields?.find { it.name == "float" }?.args
                ?: throw AssertionError("Expected non null field")

        assertThat(inputValues[0].name, equalTo("doubleIt"))
        assertThat(inputValues[0].type.ofType?.name, equalTo("Boolean"))
    }
}