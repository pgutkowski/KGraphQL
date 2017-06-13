package com.github.pgutkowski.kgraphql.specification.typesystem

import com.github.pgutkowski.kgraphql.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.util.*

@Specification("3.1.1 Scalars")
class ScalarsSpecificationTest {

    data class Person(val uuid : UUID, val name : String)

    @Test
    fun `type systems can add additional scalars with semantic meaning`(){
        val uuid = UUID.randomUUID()
        val testedSchema = KGraphQL.schema {
            scalar<UUID> {
                description = "unique identifier of object"
                serialize = { uuid : String -> UUID.fromString(uuid) }
                deserialize = UUID::toString
                validate = String::isNotBlank
            }
            query("person"){
                resolver{ -> Person(uuid, "John Smith")}
            }
            mutation("createPerson") {
                resolver{ uuid : UUID, name: String -> Person(uuid, name) }
            }
        }

        val queryResponse = deserialize(testedSchema.execute("{person{uuid}}"))
        assertThat(extract<String>(queryResponse, "data/person/uuid"), equalTo(uuid.toString()))

        val mutationResponse = deserialize(testedSchema.execute(
                        "{createPerson(uuid: $uuid, name: \"John\"){uuid name}}"
        ))
        assertThat(extract<String>(mutationResponse, "data/createPerson/uuid"), equalTo(uuid.toString()))
        assertThat(extract<String>(mutationResponse, "data/createPerson/name"), equalTo("John"))
    }

    @Test
    fun `integer value represents a value grater than 2^-31 and less or equal to 2^31`(){
        val schema = KGraphQL.schema {
            mutation("Int") {
                resolver { int : Int -> int }
            }
        }

        expect<SyntaxException>("is not valid value of type Int"){
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
        assertThat(extract<Double>(map, "data/float"), equalTo(1.0))
    }
}