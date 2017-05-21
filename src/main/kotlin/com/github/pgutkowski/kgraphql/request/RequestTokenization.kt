package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.SyntaxException

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

/**
 * splits request tokens for pair of fragments and main request graph
 */
fun split(tokens : List<String>) : Pair<List<String>, List<String>>{
    var nestedBrackets = 0
    var nestedParenthesis = 0
    var isFragment : Boolean = false

    return tokens.partition { token ->
        var closesFragment = false
        when(token){
            "{" -> nestedBrackets++
            "}" -> {
                nestedBrackets--
                if(nestedBrackets < 0){
                    throw SyntaxException("No matching opening bracket for closing bracket at ${getIndexOfTokenInString(tokens)}")
                }
                closesFragment = isFragment && nestedBrackets == 0
                if(closesFragment){
                    isFragment = false
                }
            }
            "(" -> nestedParenthesis++
            ")" -> {
                nestedParenthesis--
                if(nestedParenthesis < 0){
                    throw SyntaxException("No matching opening parenthesis for closing parenthesis at ${getIndexOfTokenInString(tokens)}")
                }
            }
            "fragment" -> {
                if(nestedBrackets == 0 && nestedParenthesis == 0){
                    isFragment = true
                }
            }
        }

        closesFragment || isFragment
    }
}

fun getIndexOfTokenInString(tokens: List<String>): Int {
    return tokens.fold(0, { index, token -> index + token.length })
}
