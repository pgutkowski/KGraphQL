package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.request.graph.Fragment

data class ParsingContext(
        val fullString : String,
        val tokens: List<String>,
        val fragments: Map<String, Fragment.External>,
        private var index : Int = 0,
        var nestedBrackets: Int = 0,
        var nestedParenthesis: Int = 0
) {
    fun index() = index

    fun next(delta: Int = 1) {
        index += delta
    }

    fun currentToken() = tokens[index]

    fun currentTokenOrNull() = tokens.getOrNull(index)

    fun peekToken(delta: Int = 1) = tokens.getOrNull(index + delta)

    fun traverseObject(): List<String> {
        val subTokens = subTokens(tokens, index, "{", "}")
        next(subTokens.size + 1)
        return subTokens
    }

    fun traverseArguments(): List<String> {
        val subTokens = subTokens(tokens, index, "(", ")")
        next(subTokens.size + 1)
        return subTokens
    }

    private fun subTokens(allTokens: List<String>, startIndex: Int, openingToken: String, closingToken: String): List<String> {
        val tokens = allTokens.subList(startIndex, allTokens.size)
        var nestedLevel = 0
        tokens.forEachIndexed { index, token ->
            when (token) {
                openingToken -> nestedLevel++
                closingToken -> nestedLevel--
            }
            if (nestedLevel == 0) return tokens.subList(1, index)
        }
        throw RequestException("Couldn't find matching closing token")
    }

    fun getFullStringIndex(tokenIndex : Int = index) : Int {
        //TODO: Provide reliable index
        return tokenIndex
    }
}