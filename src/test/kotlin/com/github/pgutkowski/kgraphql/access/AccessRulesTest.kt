package com.github.pgutkowski.kgraphql.access

import com.github.pgutkowski.kgraphql.context
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class AccessRulesTest {

    class Player(val name : String, val id : Int = 0)

    val schema = defaultSchema {
        query("black_mamba") {
            resolver { -> Player("KOBE") }
            accessRule { ctx -> if (ctx.get<String>().equals("LAKERS")) null else IllegalAccessException() }
        }

        query("white_mamba") {
            resolver { -> Player("BONNER") }
        }

        type<Player>{
            property(Player::id){
                accessRule { player, _ ->
                    if(player.name != "BONNER") IllegalAccessException("ILLEGAL ACCESS") else null
                }
            }
        }
    }


    @Test
    fun `allow when matching`(){
        val kobe = deserialize (
                schema.execute("{black_mamba{name}}", context { +"LAKERS" })
        ).extract<String>("data/black_mamba/name")

        assertThat(kobe, equalTo("KOBE"))
    }

    @Test
    fun `reject when not matching`(){
        expect<IllegalAccessException> {
            deserialize (
                    schema.execute("{ black_mamba {id} }", context { +"LAKERS" })
            ).extract<String>("data/black_mamba/id")
        }
    }

    //TODO: MORE TESTS

}