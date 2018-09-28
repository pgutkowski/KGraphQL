package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.not

val OPERANDS = "{}():[]"

val IGNORED_CHARACTERS = "\n\t, "

val DELIMITERS = OPERANDS + IGNORED_CHARACTERS

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
                val substring = input.substring(i + 1)
                val token = extractValueToken(substring)
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

private fun extractValueToken(substring: String): String {
    var index = 0
    var isEscaped = false

    val tokenBuilder = StringBuilder()

    loop@ while (index < substring.length) {
        val currentChar = substring[index]

        isEscaped = when {
            currentChar == '\"' && isEscaped.not() -> break@loop
            currentChar == '\\' -> true
            else -> false
        }

        tokenBuilder.append(currentChar)

        index++
    }

    return tokenBuilder.toString()
}

fun createDocumentTokens(tokens : List<String>) : Document {
    val operations : MutableList<Document.OperationTokens> = mutableListOf()
    val fragments : MutableList<Document.FragmentTokens> = mutableListOf()

    var index = 0
    while(index < tokens.size){
        val token = tokens[index]

        if(token == "fragment"){
            val (endIndex, fragmentTokens) = createFragmentTokens(tokens, index)
            index = endIndex
            fragments.add(fragmentTokens)
        } else {
            val (endIndex, operationTokens) = createOperationTokens(tokens, index)
            index = endIndex
            operations.add(operationTokens)
        }
    }

    return Document(fragments, operations)
}

fun createFragmentTokens(tokens : List<String>, startIndex: Int) : Pair<Int, Document.FragmentTokens>{
    var index = startIndex
    var name : String? = null
    var typeCondition : String? = null
    while(index < tokens.size){
        val token = tokens[index]
        when(token) {
            "fragment" -> {
                name = tokens[index + 1]
                index++
            }
            "on" -> {
                typeCondition = tokens[index + 1]
                index++
            }
            "{" -> {
                val indexOfClosingBracket = indexOfClosingBracket(tokens, index)
                if(name == null) throw RequestException("Invalid anonymous external fragment")
                if(typeCondition == null) throw RequestException("Invalid external fragment without type condition")
                return indexOfClosingBracket to Document.FragmentTokens(name, typeCondition, tokens.subList(index, indexOfClosingBracket))
            }
            else -> throw RequestException("Unexpected token: $token")
        }
        index++
    }
    throw RequestException("Invalid fragment $name declaration without selection set")
}

fun createOperationTokens(tokens : List<String>, startIndex: Int) : Pair<Int, Document.OperationTokens>{
    var index = startIndex
    var name : String? = null
    var type : String? = null
    var operationVariables : List<OperationVariable>? = null
    while(index < tokens.size){
        val token = tokens[index]
        when {
            token == "{" -> {
                val indexOfClosingBracket = indexOfClosingBracket(tokens, index)
                return indexOfClosingBracket to Document.OperationTokens(name, type, operationVariables?.toList(), tokens.subList(index, indexOfClosingBracket))
            }
            token == "(" -> {
                val variablesTokens = tokens.subList(index + 1, tokens.size).takeWhile { it != ")" }
                operationVariables = (parseOperationVariables(variablesTokens))
                index += variablesTokens.size +1
            }
            type == null -> {
                if(token.equals("query", true) || token.equals("mutation", true)){
                    type = token
                } else {
                    throw RequestException("Unexpected operation type $token")
                }
            }
            name == null -> name = token
            else -> throw RequestException("Unexpected token: $token")
        }
        index++
    }
    throw RequestException("Invalid operation $name without selection set")
}

private fun parseOperationVariables(variablesTokens: List<String>): MutableList<OperationVariable> {
    val operationVariables= mutableListOf<OperationVariable>()
    var variableName: String? = null
    var variableType: String? = null
    var defaultTypeStarted = false
    var variableDefaultValue: String? = null
    for(variableToken in variablesTokens) {
        when {
            variableToken == ":" -> {
                if(variableName == null) throw RequestException("Unexpected token ':', before variable name")
            }
            variableToken == "=" -> {
                if (variableName == null || variableType == null) {
                    throw RequestException("Unexpected token '=', before variable name and type declaration")
                } else {
                    defaultTypeStarted = true
                }
            }
            variableName == null -> variableName = variableToken
            variableType == null -> variableType = variableToken
            variableType.startsWith("[") && variableType == "]" -> variableType += variableToken
            variableType.startsWith("[")  -> variableType += variableToken
            defaultTypeStarted && variableDefaultValue == null -> variableDefaultValue = variableToken
            else -> {
                //if variableName of variableType would be null, it would already be matched
                operationVariables.add(OperationVariable(variableName, variableType.toTypeReference(), variableDefaultValue))
                variableName = variableToken
                variableType = null
                defaultTypeStarted = false
                variableDefaultValue = null
            }
        }
    }
    if(variableName != null && variableType != null){
        operationVariables.add(OperationVariable(variableName, variableType.toTypeReference(), variableDefaultValue))
    }
    return operationVariables
}

val TYPE_WRAPPERS = arrayOf('!', '[', ']')

fun String.toTypeReference() : TypeReference {
    val isNullable = not(endsWith("!"))
    val isList = startsWith("[") && (endsWith("]") || endsWith("]!"))
    val isElementNullable = isList && not(endsWith("!]") || endsWith("!]!"))
    val name = dropWhile { it in TYPE_WRAPPERS }.dropLastWhile { it in TYPE_WRAPPERS }
    return TypeReference(name, isNullable, isList, isElementNullable)
}

fun indexOfClosingBracket(tokens: List<String>, startIndex: Int) : Int {
    var nestedBrackets = 0
    val subList = tokens.subList(startIndex, tokens.size)
    subList.forEachIndexed { index, token ->
        when(token){
            "{" -> nestedBrackets++
            "}" -> nestedBrackets--
        }
        if(nestedBrackets == 0) return index + startIndex + 1
    }
    val indexOfTokenInString = getIndexOfTokenInString(tokens.subList(0, startIndex))
    throw RequestException("Missing closing bracket for opening bracket at $indexOfTokenInString")
}

private fun getIndexOfTokenInString(tokens: List<String>): Int {
    return tokens.fold(0, { index, token -> index + token.length })
}
