package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Ignore
import org.junit.Test

@Specification("2.9 Input Values")
class InputValuesSpecificationTest {

    enum class FakeEnum{
        ENUM1, ENUM2
    }

    data class FakeData (val number : Int, val description : String, val list : List<String> = emptyList())

    val schema = defaultSchema {
        enum<FakeEnum>()
        type<FakeData>()

        query("Int") { resolver { value: Int -> value } }
        query("Float") {resolver {value: Float -> value } }
        query("Double") {resolver {value: Double -> value } }
        query("String") {resolver {value: String -> value } }
        query("Boolean") {resolver {value: Boolean -> value } }
        query("Null") {resolver {value: Int? -> value } }
        query("Enum") {resolver {value: FakeEnum -> value } }
        query("List") {resolver {value: List<Int> -> value } }
        query("Object") {resolver {value: FakeData -> value } }
    }

    @Test
    @Specification("2.9.1 Int Value")
    fun `Int input value`(){
        val input = 4356
        val response = deserialize(schema.execute("{Int(value : $input)}"))
        assertThat(extract<Int>(response, "data/Int"), equalTo(input))
    }

    @Test
    @Specification("2.9.2 Float Value")
    fun `Float input value`(){
        val input : Double = 4356.34
        val response = deserialize(schema.execute("{Float(value : $input)}"))
        assertThat(extract<Double>(response, "data/Float"), equalTo(input))
    }

    @Test
    @Specification("2.9.2 Float Value")
    fun `Double input value`(){
        //GraphQL Float is Kotlin Double
        val input = 4356.34
        val response = deserialize(schema.execute("{Double(value : $input)}"))
        assertThat(extract<Double>(response, "data/Double"), equalTo(input))
    }

    @Test
    @Specification("2.9.2 Float Value")
    fun `Double with exponential input value`(){
        val input = 4356.34e2
        val response = deserialize(schema.execute("{Double(value : $input)}"))
        assertThat(extract<Double>(response, "data/Double"), equalTo(input))
    }

    @Test
    @Specification("2.9.4 String Value")
    fun `String input value`(){
        val input = "\\Ala ma kota \\\n\\kot ma Alę\\"
        val response = deserialize(schema.execute("{String(value : \"$input\")}"))
        assertThat(extract<String>(response, "data/String"), equalTo(input))
    }

    @Test
    @Specification("2.9.3 Boolean Value")
    fun `Boolean input value`(){
        val input = true
        val response = deserialize(schema.execute("{Boolean(value : $input)}"))
        assertThat(extract<Boolean>(response, "data/Boolean"), equalTo(input))
    }

    @Test
    @Specification("2.9.3 Boolean Value")
    fun `Invalid Boolean input value`(){
        expect<RequestException>("argument 'null' is not valid value of type Boolean"){
            deserialize(schema.execute("{Boolean(value : null)}"))
        }
    }

    @Test
    @Specification("2.9.5 Null Value")
    fun `Null input value`(){
        val response = deserialize(schema.execute("{Null(value : null)}"))
        assertThat(extract<Nothing?>(response, "data/Null"), equalTo(null))
    }

    @Test
    @Specification("2.9.6 Enum Value")
    fun `Enum input value`(){
        val response = deserialize(schema.execute("{Enum(value : ENUM1)}"))
        assertThat(extract<String>(response, "data/Enum"), equalTo(FakeEnum.ENUM1.toString()))
    }

    @Test
    @Specification("2.9.7 List Value")
    fun `List input value`(){
        val response = deserialize(schema.execute("{List(value : [23, 3, 23])}"))
        assertThat(extract<List<Int>>(response, "data/List"), equalTo(listOf(23, 3, 23)))
    }

    @Test
    @Ignore("literal object input values are not implemented yet")
    @Specification("2.9.8 Object Value")
    fun `Literal object input value`(){
        val response = deserialize(schema.execute("{Object(value: {number: 232, description: \"little number\"}){number, description}}"))
        assertThat(
                extract<Map<String, Any>>(response, "data/Object"),
                equalTo(mapOf("number" to 232, "description" to "little number"))
        )
    }

    @Test
    @Ignore("literal object input values are not implemented yet")
    @Specification("2.9.8 Object Value")
    fun `Literal object input value with list field`(){
        val response = deserialize(schema.execute(
                "{Object(" +
                        "value: {number: 232, " +
                        "description: \"little number\", " +
                        "list: [\"number\",\"description\",\"little number\",]})" +
                "{number, description}}"
        ))
        assertThat(
                extract<List<String>>(response, "data/Object/list"),
                equalTo(listOf("number", "description", "little number"))
        )
    }

    @Test
    @Specification("2.9.8 Object Value")
    fun `Object input value`(){
        val response = deserialize(schema.execute(
                "query(\$object: FakeData){Object(value: \$object){number, description}}",
                "{ \"object\" : {\"number\":232, \"description\":\"little number\"}}"
        ))
        assertThat(
                extract<Map<String, Any>>(response, "data/Object"),
                equalTo(mapOf("number" to 232, "description" to "little number"))
        )
    }

    @Test
    @Specification("2.9.8 Object Value")
    fun `Object input value with list field`(){
        val response = deserialize(schema.execute(
                "query(\$object: FakeData){Object(value: \$object){list}}",
                "{ \"object\" : {\"number\":232, \"description\":\"little number\", \"list\" : [\"number\",\"description\",\"little number\"]}}"
        ))
        assertThat(
                extract<List<String>>(response, "data/Object/list"),
                equalTo(listOf("number", "description", "little number"))
        )
    }

    @Test
    @Specification("2.9.8 Object Value")
    fun `Unknown object input value type`(){
        expect<IllegalArgumentException>("Invalid variable argument type FakeDate, expected FakeData"){
            schema.execute("query(\$object: FakeDate){Object(value: \$object){list}}")
        }
    }
}