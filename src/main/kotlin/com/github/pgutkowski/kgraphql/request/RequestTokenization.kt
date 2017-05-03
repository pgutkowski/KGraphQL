package com.github.pgutkowski.kgraphql.request

val DELIMITERS = "{}\n(): "

val IGNORED_CHARACTERS = "\n\t, "

val OPERANDS = "{}():"

fun tokenizeRequest(input : String) : List<String> {
    var i = 0
    val tokens : MutableList<String> = mutableListOf()

    while(i < input.length){
        when(input[i]){
            in IGNORED_CHARACTERS -> { i++ }
            in OPERANDS -> {
                tokens.add(input[i].toString())
                i++
            }
            '\"' -> {
                val token = input.substring(i+1).takeWhile { it != '\"' }
                i += token.length + 2 //2 for quotes
                tokens.add("\"$token\"")
            }
            else -> {
                val tokenBuilder = StringBuilder()

                while(i < input.length && input[i] !in DELIMITERS){
                    if(input[i] !in IGNORED_CHARACTERS) tokenBuilder.append(input[i])
                    i++
                }

                if(tokenBuilder.isNotBlank()) tokens.add(tokenBuilder.toString())
            }
        }
    }

    return tokens
}
