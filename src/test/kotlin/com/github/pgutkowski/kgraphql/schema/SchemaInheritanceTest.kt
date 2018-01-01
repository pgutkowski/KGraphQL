package com.github.pgutkowski.kgraphql.schema

import com.github.pgutkowski.kgraphql.KGraphQL
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.expect
import org.junit.Test
import java.util.*


class SchemaInheritanceTest {

    open class A (open var name : String = "", open var age : Int = 0) {
        var id : String = UUID.randomUUID().toString()
    }

    class B (name: String, age: Int, var pesel : String = "") : A(name, age)

    class C (override var name: String, override var age: Int, var pesel : String = "") : A(name, age)

    @Test
    fun `call to ignore property should cascade to subclasses`(){
        val name = "PELE"
        val age = 20

        val schema = KGraphQL.schema {

            type<A>{ A::id.ignore() }

            query("b") { -> B(name, age) }

            query("c") { -> C(name, age) }
        }

        expect<RequestException>("property id on B does not exist") {
            deserialize(schema.execute("{b{id, name, age}}"))
        }

        expect<RequestException>("property id on C does not exist") {
            deserialize(schema.execute("{c{id, name, age}}"))
        }
    }

}