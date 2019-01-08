package com.github.pgutkowski.kgraphql.specification.typesystem

import com.github.pgutkowski.kgraphql.KGraphQL
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import com.github.pgutkowski.kgraphql.schema.scalar.StringScalarCoercion
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.time.LocalDate
import java.util.*

@Specification("3.1.1 Scalars")
class ScalarsSpecificationTest {

    data class Person(val uuid: UUID, val name: String)

    @Test
    fun `type systems can add additional scalars with semantic meaning`() {
        val uuid = UUID.randomUUID()
        val testedSchema = KGraphQL.schema {
            stringScalar<UUID> {
                description = "unique identifier of object"

                coercion = object : StringScalarCoercion<UUID> {
                    override fun serialize(instance: UUID): String = instance.toString()

                    override fun deserialize(raw: String): UUID = UUID.fromString(raw)
                }
            }
            query("person") {
                resolver { -> Person(uuid, "John Smith") }
            }
            mutation("createPerson") {
                resolver { uuid: UUID, name: String -> Person(uuid, name) }
            }
        }

        val queryResponse = deserialize(testedSchema.execute("{person{uuid}}"))
        assertThat(queryResponse.extract<String>("data/person/uuid"), equalTo(uuid.toString()))

        val mutationResponse = deserialize(testedSchema.execute(
                "{createPerson(uuid: \"$uuid\", name: \"John\"){uuid name}}"
        ))
        assertThat(mutationResponse.extract<String>("data/createPerson/uuid"), equalTo(uuid.toString()))
        assertThat(mutationResponse.extract<String>("data/createPerson/name"), equalTo("John"))
    }

    @Test
    fun `integer value represents a value grater than 2^-31 and less or equal to 2^31`() {
        val schema = KGraphQL.schema {
            mutation("Int") {
                resolver { int: Int -> int }
            }
        }

        expect<RequestException>("is not valid value of type Int") {
            schema.execute("{Int(int: ${Integer.MAX_VALUE.toLong() + 2L})}")
        }
    }

    @Test
    fun `when float is expected as an input type, both integer and float input values are accepted`() {
        val schema = KGraphQL.schema {
            mutation("float") {
                resolver { float: Float -> float }
            }
        }
        val map = deserialize(schema.execute("{float(float: 1)}"))
        assertThat(map.extract<Double>("data/float"), equalTo(1.0))
    }

    @Test
    fun `server can declare custom ID type`() {
        val testedSchema = KGraphQL.schema {
            stringScalar<UUID> {
                name = "ID"
                description = "unique identifier of object"
                deserialize = { uuid: String -> UUID.fromString(uuid) }
                serialize = UUID::toString
            }
            query("personById") {
                resolver { id: UUID -> Person(id, "John Smith") }
            }
        }

        val randomUUID = UUID.randomUUID()
        val map = deserialize(testedSchema.execute("query(\$id: ID = \"$randomUUID\"){personById(id: \$id){uuid, name}}"))
        assertThat(map.extract<String>("data/personById/uuid"), equalTo(randomUUID.toString()))
    }


    @Test
    fun `For numeric scalars, input string with numeric content must raise a query error indicating an incorrect type`() {
        val schema = KGraphQL.schema {
            mutation("Int") {
                resolver { int: Int -> int }
            }
        }

        expect<RequestException>("") {
            schema.execute("{Int(int: \"223\")}")
        }
    }

    data class Number(val int: Int)

    @Test
    fun `Schema may declare custom int scalar type`() {

        val schema = KGraphQL.schema {
            intScalar<Number> {
                deserialize = ::Number
                serialize = { (int) -> int }
            }

            query("number") {
                resolver { number: Number -> number }
            }
        }

        val value = 3434
        val response = deserialize(schema.execute("{number(number: $value)}"))
        assertThat(response.extract<Int>("data/number"), equalTo(value))
    }

    data class Bool(val boolean: Boolean)

    @Test
    fun `Schema may declare custom boolean scalar type`() {

        val schema = KGraphQL.schema {
            booleanScalar<Bool> {
                deserialize = ::Bool
                serialize = { (boolean) -> boolean }
            }

            query("boolean") {
                resolver { boolean: Boolean -> boolean }
            }
        }

        val value = true
        val response = deserialize(schema.execute("{boolean(boolean: $value)}"))
        assertThat(response.extract<Boolean>("data/boolean"), equalTo(value))
    }

    data class Boo(val boolean: Boolean)
    data class Lon(val long: Long)
    data class Dob(val double: Double)
    data class Num(val int: Int)
    data class Str(val string: String)
    data class Multi(val boo: Boo, val str: String, val num: Num)

    @Test
    fun `Schema may declare custom double scalar type`() {

        val schema = KGraphQL.schema {
            floatScalar<Dob> {
                deserialize = ::Dob
                serialize = { (double) -> double }
            }

            query("double") {
                resolver { double: Dob -> double }
            }
        }

        val value = 232.33
        val response = deserialize(schema.execute("{double(double: $value)}"))
        assertThat(response.extract<Double>("data/double"), equalTo(value))
    }

    @Test
    fun `Scalars within input variables`() {
        val schema = KGraphQL.schema {
            booleanScalar<Boo> {
                deserialize = ::Boo
                serialize = { (boolean) -> boolean }
            }
            longScalar<Lon> {
                deserialize = ::Lon
                serialize = { (long) -> long }
            }
            floatScalar<Dob> {
                deserialize = ::Dob
                serialize = { (double) -> double }
            }
            intScalar<Num> {
                deserialize = ::Num
                serialize = { (num) -> num }
            }
            stringScalar<Str> {
                deserialize = ::Str
                serialize = { (str) -> str }
            }

            query("boo") { resolver { boo: Boo -> boo } }
            query("lon") { resolver { lon: Lon -> lon } }
            query("dob") { resolver { dob: Dob -> dob } }
            query("num") { resolver { num: Num -> num } }
            query("str") { resolver { str: Str -> str } }
            query("multi") { resolver { -> Multi(Boo(false), "String", Num(25)) } }
        }

        val booValue = true
        val lonValue = 124L
        val dobValue = 2.5
        val numValue = 155
        val strValue = "Test"
        val d = '$'

        val req = """
            query Query(${d}boo: Boo!, ${d}lon: Lon!, ${d}dob: Dob!, ${d}num: Num!, ${d}str: Str!){
                boo(boo: ${d}boo)
                lon(lon: ${d}lon)
                dob(dob: ${d}dob)
                num(num: ${d}num)
                str(str: ${d}str)
                multi { boo, str, num }
            }
        """.trimIndent()

        val values = """
            {
                "boo": $booValue,
                "lon": $lonValue,
                "dob": $dobValue,
                "num": $numValue,
                "str": "$strValue"
            }
        """.trimIndent()

        val response = deserialize(schema.execute(req, values))
        assertThat(response.extract<Boolean>("data/boo"), equalTo(booValue))
        assertThat(response.extract<Int>("data/lon"), equalTo(lonValue.toInt()))
        assertThat(response.extract<Double>("data/dob"), equalTo(dobValue))
        assertThat(response.extract<Int>("data/num"), equalTo(numValue))
        assertThat(response.extract<String>("data/str"), equalTo(strValue))

        assertThat(response.extract<Boolean>("data/multi/boo"), equalTo(false))
        assertThat(response.extract<String>("data/multi/str"), equalTo("String"))
        assertThat(response.extract<Int>("data/multi/num"), equalTo(25))
    }

    data class NewPart(val manufacturer: String, val name: String, val oem: Boolean, val addedDate: LocalDate)

    @Test
    fun `Schema may declare LocalDate custom scalar`() {
        val schema = KGraphQL.schema {
            stringScalar<LocalDate> {
                serialize = { date -> date.toString() }
                deserialize = { dateString -> LocalDate.parse(dateString) }
                description = "Date in format yyyy-mm-dd"
            }

            mutation("addPart") {
                description = "Adds a new part in the parts inventory database"
                resolver { newPart: NewPart ->
                    println(newPart)

                    newPart
                }
            }

            inputType<NewPart> {}
        }

        val manufacturer = """Joe Bloggs"""

        val response = deserialize(schema.execute(
                "mutation Mutation(\$newPart : NewPart!){ addPart(newPart: \$newPart) {manufacturer} }",
                """
                    { "newPart" : {
                      "manufacturer":"$manufacturer",
                      "name":"Front bumper",
                      "oem":true,
                      "addedDate":"2001-09-01"
                    }}
                """.trimIndent())
        )

        assertThat(response.extract<String>("data/addPart/manufacturer"), equalTo(manufacturer))
    }
}