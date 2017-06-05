package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Specification("2.9 Input Values")
class InputValuesSpecificationTest {

    enum class FakeEnum{
        ENUM1, ENUM2
    }

    val schema = defaultSchema {
        enum<FakeEnum>()

        query("Int") { resolver { value: Int -> value } }
        query("Float") {resolver {value: Float -> value } }
        query("Double") {resolver {value: Double -> value } }
        query("String") {resolver {value: String -> value } }
        query("Boolean") {resolver {value: Boolean -> value } }
        query("Null") {resolver {value: Int? -> value } }
        query("Enum") {resolver {value: FakeEnum -> value } }
        query("List") {resolver {value: List<Int> -> value } }
        query("Object") {resolver {value: Actor -> value } }
    }

    @Test
    fun `Int input value`(){
        val input = 4356
        val response = deserialize(schema.execute("{Int(value : $input)}"))
        assertThat(extract<Int>(response, "data/Int"), equalTo(input))
    }

    @Test
    @Disabled("Strategy regarding float and double has to be stated")
    fun `Float input value`(){
        val input : Float = 4356.34F
        val response = deserialize(schema.execute("{Float(value : $input)}"))
        assertThat(extract<Float>(response, "data/Float"), equalTo(input))
    }

    @Test
    fun `Double input value`(){
        val input = 4356.34
        val response = deserialize(schema.execute("{Double(value : $input)}"))
        assertThat(extract<Double>(response, "data/Double"), equalTo(input))
    }

    @Test
    fun `Double with exponential input value`(){
        val input = 4356.34e2
        val response = deserialize(schema.execute("{Double(value : $input)}"))
        assertThat(extract<Double>(response, "data/Double"), equalTo(input))
    }

    @Test
    fun `String input value`(){
        val input = "\\Ala ma kota \\\n\\kot ma Alę\\"
        val response = deserialize(schema.execute("{String(value : \"$input\")}"))
        assertThat(extract<String>(response, "data/String"), equalTo(input))
    }

    @Test
    fun `Boolean input value`(){
        val input = true
        val response = deserialize(schema.execute("{Boolean(value : $input)}"))
        assertThat(extract<Boolean>(response, "data/Boolean"), equalTo(input))
    }

    @Test
    fun `Invalid Boolean input value`(){
        expect<SyntaxException>("argument 'null' is not valid value of type Boolean"){
            deserialize(schema.execute("{Boolean(value : null)}"))
        }
    }

    @Test
    fun `Null input value`(){
        val response = deserialize(schema.execute("{Null(value : null)}"))
        assertThat(extract<Nothing?>(response, "data/Null"), equalTo(null))
    }

    @Test
    fun `Enum input value`(){
        val response = deserialize(schema.execute("{Enum(value : ENUM1)}"))
        assertThat(extract<String>(response, "data/Enum"), equalTo(FakeEnum.ENUM1.toString()))
    }

    @Test
    @Disabled
    fun `List input value`(){
        val response = deserialize(schema.execute("{List(value : [23, 3, 23])}"))
        assertThat(extract<List<Int>>(response, "data/List"), equalTo(listOf(23, 3, 23)))
    }
}