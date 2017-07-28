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
import java.util.*

@Specification("3.1.1 Scalars")
class ScalarsSpecificationTest {

    data class Person(val uuid : UUID, val name : String)

    @Test
    fun `type systems can add additional scalars with semantic meaning`(){
        val uuid = UUID.randomUUID()
        val testedSchema = KGraphQL.schema {
            stringScalar<UUID> {
                description = "unique identifier of object"

                coercion = object : StringScalarCoercion<UUID>{
                    override fun serialize(instance: UUID): String = instance.toString()

                    override fun deserialize(raw: String): UUID = UUID.fromString(raw)
                }
            }
            query("person"){
                resolver{ -> Person(uuid, "John Smith")}
            }
            mutation("createPerson") {
                resolver{ uuid : UUID, name: String -> Person(uuid, name) }
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
    fun `integer value represents a value grater than 2^-31 and less or equal to 2^31`(){
        val schema = KGraphQL.schema {
            mutation("Int") {
                resolver { int : Int -> int }
            }
        }

        expect<RequestException>("is not valid value of type Int"){
            schema.execute("{Int(int: ${Integer.MAX_VALUE.toLong()+2L})}")
        }
    }

    @Test
    fun `when float is expected as an input type, both integer and float input values are accepted`(){
        val schema = KGraphQL.schema {
            mutation("float"){
                resolver { float: Float -> float }
            }
        }
        val map = deserialize(schema.execute("{float(float: 1)}"))
        assertThat(map.extract<Double>("data/float"), equalTo(1.0))
    }

    @Test
    fun `server can declare custom ID type`(){
        val testedSchema = KGraphQL.schema {
            stringScalar<UUID> {
                name = "ID"
                description = "unique identifier of object"
                deserialize = { uuid : String -> UUID.fromString(uuid) }
                serialize = UUID::toString
            }
            query("personById"){
                resolver{ id: UUID -> Person(id, "John Smith")}
            }
        }

        val randomUUID = UUID.randomUUID()
        val map = deserialize(testedSchema.execute("query(\$id: ID = \"$randomUUID\"){personById(id: \$id){uuid, name}}"))
        assertThat(map.extract<String>("data/personById/uuid"), equalTo(randomUUID.toString()))
    }


    @Test
    fun `For numeric scalars, input string with numeric content must raise a query error indicating an incorrect type`(){
        val schema = KGraphQL.schema {
            mutation("Int") {
                resolver { int : Int -> int }
            }
        }

        expect<RequestException>(""){
            schema.execute("{Int(int: \"223\")}")
        }
    }

    data class Number(val int : Int)

    @Test
    fun `Schema may declare custom int scalar type`(){

        val schema = KGraphQL.schema {
            intScalar<Number> {
                deserialize = ::Number
                serialize = { (int) -> int }
            }

            query("number"){
                resolver { number : Number -> number }
            }
        }

        val value = 3434
        val response = deserialize(schema.execute("{number(number: $value)}"))
        assertThat(response.extract<Int>("data/number"), equalTo(value))
    }

    data class Bool(val boolean: Boolean)

    @Test
    fun `Schema may declare custom boolean scalar type`(){

        val schema = KGraphQL.schema {
            booleanScalar<Bool> {
                deserialize = ::Bool
                serialize = { (boolean) -> boolean }
            }

            query("boolean"){
                resolver { boolean : Boolean -> boolean }
            }
        }

        val value = true
        val response = deserialize(schema.execute("{boolean(boolean: $value)}"))
        assertThat(response.extract<Boolean>("data/boolean"), equalTo(value))
    }

    data class Dob(val double : Double)

    @Test
    fun `Schema may declare custom double scalar type`(){

        val schema = KGraphQL.schema {
            floatScalar<Dob> {
                deserialize = ::Dob
                serialize = { (double) -> double }
            }

            query("double"){
                resolver { double : Dob -> double }
            }
        }

        val value = 232.33
        val response = deserialize(schema.execute("{double(double: $value)}"))
        assertThat(response.extract<Double>("data/double"), equalTo(value))
    }
}