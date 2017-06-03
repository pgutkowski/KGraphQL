package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

@Specification("2.6 Arguments")
class ArgumentsSpecificationTest {
    val age = 432

    val schema = defaultSchema {
        query {
            name = "actor"
            resolver { -> Actor("Boguś Linda", age) }
        }

        type<Actor>{
            property<List<String>> {
                name = "favDishes"
                resolver { actor: Actor, size: Int, prefix: String? ->
                    listOf("steak", "burger", "soup", "salad", "bread", "bird").let { dishes ->
                        if(prefix != null){
                            dishes.filter { it.startsWith(prefix) }
                        } else {
                            dishes
                        }
                    }.take(size)
                }
            }
        }
    }

    @Test
    fun `arguments are unordered`(){
        executeEqualQueries( schema,
                mapOf("data" to mapOf("actor" to mapOf("favDishes" to listOf("burger", "bread")))),
                "{actor{favDishes(size: 2, prefix: \"b\")}}",
                "{actor{favDishes(prefix: \"b\", size: 2)}}"
        )
    }

    @Test
    fun `many arguments can exist on given field`(){
        val response = deserialize(schema.execute("{actor{favDishes(size: 2, prefix: \"b\")}}")) as Map<String, Any>
        assertThat (
                response, equalTo(mapOf<String, Any>("data" to mapOf("actor" to mapOf("favDishes" to listOf("burger", "bread")))))
        )
    }


}
