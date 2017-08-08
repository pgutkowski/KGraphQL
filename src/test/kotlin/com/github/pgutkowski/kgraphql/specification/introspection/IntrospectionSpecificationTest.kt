package com.github.pgutkowski.kgraphql.specification.introspection

import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import com.github.pgutkowski.kgraphql.integration.BaseSchemaTest
import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.introspection.TypeKind
import junit.framework.Assert.fail
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.collection.IsEmptyCollection.empty
import org.junit.Ignore
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

        val dataReturnType = schema.queryType.fields?.find { it.name == "data" }?.type!!
        assertThat(dataReturnType.kind, equalTo(TypeKind.NON_NULL))
        assertThat(dataReturnType.ofType?.kind, equalTo(TypeKind.LIST))
        assertThat(dataReturnType.ofType?.ofType?.kind, equalTo(TypeKind.NON_NULL))
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

    interface Inter {
        val value: String
    }

    class Face(override val value: String, override val value2 : Boolean = false) : InterInter

    @Test
    fun `__typename returns actual type of object`(){
        val schema = defaultSchema {
            query("interface"){
                resolver { -> Face("~~MOCK~~") as Inter }
            }

            type<Inter>()
            type<Face>()
        }


        val response = deserialize(schema.execute("{interface{value, __typename ... on Face{value2}}}"))
        assertThat(response.extract("data/interface/__typename"), equalTo("Face"))
        assertThat(response.extract("data/interface/value2"), equalTo(false))
    }

    interface InterInter : Inter {
        val value2 : Boolean
    }

    @Test
    fun `Interfaces are supported in introspection`(){
        val schema = defaultSchema {
            query("interface"){
                resolver { -> Face("~~MOCK~~") }
            }

            type<Inter>()
            type<InterInter>()
            type<Face>()
        }


        val possibleTypes = schema.findTypeByName("Inter")?.possibleTypes?.map { it.name }
        assertThat(possibleTypes, equalTo(listOf<String?>("Face")))

        val interfaces = schema.findTypeByName("Face")?.interfaces?.map { it.name }
        assertThat(interfaces, equalTo(listOf<String?>("Inter", "InterInter")))
    }

    data class Book(val id: String)

    @Test
    @Ignore("Union types are not introspected yet")
    fun `union types possible types are supported`(){
        val schema = defaultSchema {
            query("interface"){
                resolver { -> Face("~~MOCK~~") }
            }

            type<Face>{
                unionProperty("union"){
                    returnType = unionType("FaceBook"){
                        type<Face>()
                        type<Book>()
                    }

                    resolver { it -> Book(it.value) }
                }
            }
        }


        val possibleTypes = schema.findTypeByName("FaceBook")?.possibleTypes?.map { it.name }
        assertThat(possibleTypes, equalTo(listOf<String?>("Face", "Book")))
    }

    @Test
    fun `introspection field __typename must not leak into schema introspection`(){
        val schema = defaultSchema {
            query("interface"){
                resolver { -> Face("~~MOCK~~") }
            }
        }

        val map = deserialize(schema.execute(BaseSchemaTest.INTROSPECTION_QUERY))
        val fields = map.extract<List<Map<String,*>>>("data/__schema/types[0]/fields")

        fields.forEach { field ->
            assertThat(field["name"] as String, not(startsWith("__")))
        }
    }

    @Test
    fun `introspection types should not contain duplicated float type for kotlin Double and Float`(){
        val schema = defaultSchema {
            query("interface"){
                resolver { -> Face("~~MOCK~~") }
            }
        }

        val map = deserialize(schema.execute(BaseSchemaTest.INTROSPECTION_QUERY))
        val types = map.extract<List<Map<Any,*>>>("data/__schema/types")

        val typenames = types.map { type -> type["name"] as String }.sorted()

        for(i in typenames.indices){
            if(typenames[i] == typenames.getOrNull(i + 1)) fail()
        }
    }

    /**
     * Not part of spec, but assumption of many graphql tools
     */
    @Test
    fun `query type should have non null, empty interface list`(){
        val schema = defaultSchema {
            query("interface"){
                resolver { -> Face("~~MOCK~~") }
            }
        }

        val response = deserialize(schema.execute("{__schema{queryType{interfaces{name}}}}"))
        assertThat(response.extract<List<*>>("data/__schema/queryType/interfaces"), empty())
    }

    /**
     * Not part of spec, but assumption of many graphql tools
     */
    @Test
    fun `__Directive introspection type should have onField, onFragment, onOperation fields`(){
        val schema = defaultSchema {
            query("interface"){
                resolver { -> Face("~~MOCK~~") }
            }
        }

        val response = deserialize(schema.execute("{__schema{directives{name, onField, onFragment, onOperation}}}"))
        val directives = response.extract<List<Map<String, *>>>("data/__schema/directives")
        directives.forEach { directive ->
            assertThat(directive["name"] as String, anyOf(equalTo("skip"), equalTo("include")))
            assertThat(directive["onField"] as Boolean, equalTo(true))
            assertThat(directive["onFragment"] as Boolean, equalTo(true))
            assertThat(directive["onOperation"] as Boolean, equalTo(false))
        }
    }
}