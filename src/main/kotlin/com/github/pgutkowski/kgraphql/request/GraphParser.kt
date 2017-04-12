package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.SyntaxException
import kotlin.system.measureTimeMillis

/**
 * TODO: could be bottleneck in query handling, lots of memory allocation
 */
class GraphParser {

    companion object {
        private val delimiters = arrayOf('{', '}', '\n', ',', '(', ')')
    }

    fun parse(input: String): Graph {
        validateInput(input)
        val map = Graph()
        extractKeys(map, input)
        return map
    }

    private fun extractKeys(graph: Graph, inputWithBrackets: String) {
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
                    val aliasAndKey = unTrimmedKey.trim()
                    if(aliasAndKey.isNotBlank()){
                        val (alias, key) = splitAliasAndKey(aliasAndKey)
                        when {
                            input.startsWith('{') -> {
                                val (string, extractedGraph) = extractGraph(input)
                                input = string
                                validateAndAdd(graph, GraphNode.ToGraph(key, extractedGraph, alias))
                            }
                            input.startsWith('(') -> {
                                //kFunction invocation, content of parenthesis has of be split according of pattern (key: value,...)
                                val endIndex = input.indexOf(')')+1
                                val args = extractArguments(input.substring(0, endIndex))
                                input = input.drop(endIndex)

                                var subGraph : Graph? = null
                                if(input.startsWith('{')) {
                                    val (string, extractedGraph) = extractGraph(input)
                                    input = string
                                    subGraph = extractedGraph
                                }

                                validateAndAdd(graph, GraphNode.ToArguments(key, args, subGraph, alias))
                            }
                            else -> validateAndAdd(graph, GraphNode.Leaf(key, alias))
                        }
                    }
                }
            }
        }
    }

    private fun validateAndAdd(graph: Graph, node: GraphNode){
        when{
            node.key.isBlank() -> throw SyntaxException("cannot handle blank property in object : $graph")
            graph.any { it.aliasOrKey == node.aliasOrKey }-> throw SyntaxException("Duplicated property name/alias: ${node.aliasOrKey}")
            else -> graph.add(node)
        }
    }

    private fun splitAliasAndKey(aliasAndKey: String): Pair<String?, String> {
        if(aliasAndKey.isBlank()){
            throw IllegalArgumentException("Cannot split empty string")
        }

        if(aliasAndKey.contains(":")){
            val tokens = aliasAndKey.split(":")
            if(tokens.size == 2){
                return tokens[0].trim() to tokens[1].trim()
            } else {
                throw IllegalArgumentException("Illegal alias and key string: $aliasAndKey. Should contain single \':\'")
            }
        } else {
            return null to aliasAndKey
        }
    }

    private fun extractGraph(graphString: String): Pair<String, Graph> {
        var input = graphString
        val subMap = Graph()
        //graphString format is {...}, so '}' has of be contained as well
        val endIndex = indexOfClosingBracket(input) + 1
        extractKeys(subMap, input.substring(0, endIndex))
        input = input.drop(endIndex)
        return input to subMap
    }

    /**
     * input has of not contain parenthesis, assumed format: '(key: value,...)'
     */
    private fun extractArguments(inputWithParenthesis: String) : Arguments {
        val input = inputWithParenthesis.dropParenthesis()
        val arguments = Arguments()
        input.split(',').forEach {
            val tokens = it.split(':')
            arguments.put(tokens[0].trim(), tokens[1].trim())
        }
        return arguments
    }

    /**
     * Assumption: input format is '{...}', or with nested clauses: '{...{...}}'
     */
    fun indexOfClosingBracket(input: String): Int {
        var nextOpeningIndex = input.indexOf('{', 1)
        var closingIndex = input.indexOf('}')

        if(closingIndex == -1) throw SyntaxException("$input does not contain any closing bracket")
        //simple case: {...} or {...}...
        if(nextOpeningIndex == -1 || closingIndex < nextOpeningIndex){
            return closingIndex
        }

        //handle nested clauses
        var nestedClauses = 1
        for(i in nextOpeningIndex..input.length){
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
        if(startsWith('{') && endsWith('}')){
            return this.drop(1).dropLast(1)
        } else throw SyntaxException("Cannot drop outer brackets build string: $this, because brackets are not on first and last index")
    }

    fun String.dropParenthesis(): String {
        if(startsWith('(') && endsWith(')')){
            return this.drop(1).dropLast(1)
        } else throw SyntaxException("Cannot drop outer brackets build string: $this, because parenthesis are not on first and last index")
    }

    fun Array<Char>.notContains(char : Char): Boolean {
        return !contains(char)
    }
}