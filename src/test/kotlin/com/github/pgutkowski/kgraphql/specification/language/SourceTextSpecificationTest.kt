package com.github.pgutkowski.kgraphql.specification.language

import com.github.pgutkowski.kgraphql.Actor
import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.Specification
import com.github.pgutkowski.kgraphql.assertNoErrors
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.executeEqualQueries
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

@Specification("2.1. Source Text")
class SourceTextSpecificationTest {

    val schema = defaultSchema {
        query("fizz") {
            resolver{ -> "buzz"}
        }

        query("actor") {
            resolver { -> Actor("Bogusław Linda", 65) }
        }
    }

    @Test
    fun `invalid unicode character`() {
        expect<RequestException>("Illegal character: \\u0003"){
            deserialize(schema.execute("\u0003"))
        }
    }

    @Test
    @Specification("2.1.1 Unicode")
    fun `ignore unicode BOM character`() {
        val map = deserialize(schema.execute("\uFEFF{fizz}"))
        assertNoErrors(map)
        assertThat(map.extract<String>("data/fizz"), equalTo("buzz"))
    }

    @Test
    @Specification (
            "2.1.2 White Space",
            "2.1.3 Line Terminators",
            "2.1.5 Insignificant Commas",
            "2.1.7 Ignored Tokens"
    )
    fun `ignore whitespace, line terminator, comma characters`(){
        executeEqualQueries( schema,
                mapOf("data" to mapOf(
                        "fizz" to "buzz",
                        "actor" to mapOf("name" to "Bogusław Linda")
                )),

                "{fizz \nactor,{,\nname}}\n",
                "{fizz \tactor,  \n,\n{name}}",
                "{fizz\n actor\n{name,\n\n\n}}",
                "{\n\n\nfizz, \nactor{,name\t}\t}",
                "{\nfizz, actor,\n{\nname\t}}",
                "{\nfizz, ,actor\n{\nname,\t}}",
                "{\nfizz ,actor\n{\nname,\t}}",
                "{\nfizz, actor\n{\nname\t}}",
                "{\tfizz actor\n{name}}"
        )
    }

    @Test
    @Specification("2.1.4 Comments")
    fun `support comments`(){
        executeEqualQueries( schema,
                mapOf("data" to mapOf (
                        "fizz" to "buzz",
                        "actor" to mapOf("name" to "Bogusław Linda")
                )),

                "{fizz #FIZZ COMMENTS\nactor,{,\nname}}\n",
                "#FIZZ COMMENTS\n{fizz \tactor#FIZZ COMMENTS\n,  #FIZZ COMMENTS\n\n#FIZZ COMMENTS\n,\n{name}}",
                "{fizz\n actor\n{name,\n\n\n}}",
                "#FIZZ COMMENTS\n{\n\n\nfizz, \nactor{,name\t}\t}#FIZZ COMMENTS\n",
                "{\nfizz, actor,\n{\n#FIZZ COMMENTS\nname\t}}",
                "{\nfizz, ,actor\n{\nname,\t}}",
                "#FIZZ COMMENTS\n{\nfizz ,actor#FIZZ COMMENTS\n\n{\nname,\t}}",
                "{\nfizz,#FIZZ COMMENTS\n#FIZZ COMMENTS\n actor\n{\nname\t}}",
                "{\tfizz #FIZZ COMMENTS\nactor\n{name}#FIZZ COMMENTS\n}"
        )
    }

    @Test
    @Specification("2.1.9 Names")
    fun `names are case sensitive`(){
        expect<RequestException>("FIZZ is not supported by this schema"){
            deserialize(schema.execute("{FIZZ}"))
        }

        expect<RequestException>("Fizz is not supported by this schema"){
            deserialize(schema.execute("{Fizz}"))
        }

        val mapLowerCase = deserialize(schema.execute("{fizz}"))
        assertNoErrors(mapLowerCase)
        assertThat(mapLowerCase.extract<String>("data/fizz"), equalTo("buzz"))
    }
}