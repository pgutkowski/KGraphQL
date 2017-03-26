package com.github.pgutkowski.kql.request.serialization

import com.github.pgutkowski.kql.request.Graph
import com.github.pgutkowski.kql.request.SyntaxException

/**
 * TODO: resolve vulnerability for special characters in keys
 * TODO: probably it will be bottleneck in query handling, lots of memory allocation
 */
class DefaultGraphDeserializer : GraphDeserializer {

    companion object {
        private val delimiters = arrayOf('{', '}', '\n', ',')
    }

    override fun deserialize(input: String): Graph {
        validateInput(input)

        val map = Graph()
        extractKeys(map, input)

        return map
    }

    private fun extractKeys(map: Graph, inputWithBrackets: String) {
        var input = inputWithBrackets.dropBrackets()

        while(input.isNotBlank()){
            when {
                input.startsWith(',') -> {
                    input = input.removePrefix(",").trim()
                }
                input.startsWith('}') -> {
                    throw IllegalArgumentException("No matching opening bracket for closing bracket at \"...$input\"")
                }
                else -> {
                    val unTrimmedKey = input.takeWhile { delimiters.notContains(it) }
                    input = input.removePrefix(unTrimmedKey).trim()
                    val key = unTrimmedKey.trim()
                    var value : Any? = null
                    if(input.startsWith('{')) {
                        val subMap = Graph()
                        //input format is {...}, so '}' has to be contained as well
                        val i = indexOfClosingBracket(input)+1
                        extractKeys(subMap, input.substring(0, i))
                        value = subMap
                        input = input.drop(i)
                    }
                    if(key.isNotBlank()) map.put(key, value)
                }
            }
        }
    }

    /**
     * Assumption: input format is '{...}', or with nested clauses: '{...{...}}'
     */
    fun indexOfClosingBracket(input: String): Int {
        //drop first '{'
        var openingIndex = input.indexOf('{', 1)
        var closingIndex = input.indexOf('}')

        if(closingIndex == -1) throw SyntaxException("$input does not contain any closing bracket")
        //simple case: {...} or {...}...
        if(openingIndex == -1 || closingIndex < openingIndex){
            return closingIndex
        }

        //handle nested clauses
        var nestedClauses = 1
        for(i in openingIndex..input.length){
            when(input[i]){
                '{' -> ++nestedClauses
                '}' -> --nestedClauses
            }
            if(nestedClauses == 0){
                return i
            }
        }
        throw SyntaxException("$input does not contain matching closing bracket")
    }

    private fun validateInput(string: String) {
        val trimmedString = string.trim()
        if (!trimmedString.startsWith("{") || !trimmedString.endsWith("}")) {
            throw SyntaxException("passed string $string does not represent valid MultiMap")
        }
    }

    fun String.dropBrackets(): String {
        if(startsWith("{") && endsWith("}")){
            return this.drop(1).dropLast(1)
        } else throw SyntaxException("Cannot drop outer brackets from string: $this, because brackets are not on first and last index")
    }

    fun Array<Char>.notContains(char : Char): Boolean {
        return !contains(char)
    }
}