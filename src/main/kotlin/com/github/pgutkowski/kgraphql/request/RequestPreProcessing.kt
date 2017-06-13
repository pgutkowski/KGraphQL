package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.SyntaxException

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
                if(name == null) throw SyntaxException("Invalid anonymous external fragment")
                if(typeCondition == null) throw SyntaxException("Invalid external fragment without type condition")
                return indexOfClosingBracket to Document.FragmentTokens(name, typeCondition, tokens.subList(index, indexOfClosingBracket))
            }
            else -> throw SyntaxException("Unexpected token: $token")
        }
        index++
    }
    throw SyntaxException("Invalid fragment $name declaration without selection set")
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
                    throw SyntaxException("Unexpected operation type $token")
                }
            }
            name == null -> name = token
            else -> throw SyntaxException("Unexpected token: $token")
        }
        index++
    }
    throw SyntaxException("Invalid operation $name without selection set")
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
                if(variableName == null) throw SyntaxException("Unexpected token ':', before variable name")
            }
            variableToken == "=" -> {
                if (variableName == null || variableType == null) {
                    throw SyntaxException("Unexpected token '=', before variable name and type declaration")
                } else {
                    defaultTypeStarted = true
                }
            }
            variableName == null -> variableName = variableToken
            variableType == null -> variableType = variableToken
            defaultTypeStarted && variableDefaultValue == null -> variableDefaultValue = variableToken
            else -> {
                //if variableName of variableType would be null, it would already be matched
                operationVariables.add(OperationVariable(variableName, variableType, variableDefaultValue))
                variableName = variableToken
                variableType = null
                defaultTypeStarted = false
                variableDefaultValue = null
            }
        }
    }
    if(variableName != null && variableType != null){
        operationVariables.add(OperationVariable(variableName, variableType, variableDefaultValue))
    }
    return operationVariables
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
    throw SyntaxException("Missing closing bracket for opening bracket at $indexOfTokenInString")
}

private fun getIndexOfTokenInString(tokens: List<String>): Int {
    return tokens.fold(0, { index, token -> index + token.length })
}
