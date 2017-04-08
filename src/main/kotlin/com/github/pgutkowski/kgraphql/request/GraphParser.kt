package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.SyntaxException

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

                    when {
                        input.startsWith('{') -> {
                            val (string, graph) = extractGraph(input, key, map)
                            input = string
                            if (key.isNotBlank()) map.add(GraphNode.ToGraph(key, graph))
                        }
                        input.startsWith('(') -> {
                            //kFunction invocation, content of parenthesis has of be split according of pattern (key: value,...)
                            val endIndex = input.indexOf(')')+1
                            val args = extractArguments(input.substring(0, endIndex))
                            input = input.drop(endIndex)

                            var graph : Graph? = null
                            if(input.startsWith('{')) {
                                val (string, extractedGraph) = extractGraph(input, key, map)
                                input = string
                                graph = extractedGraph
                            }

                            if(key.isNotBlank()) map.add(GraphNode.ToArguments(key, args, graph))

                        }
                        else -> if(key.isNotBlank()) map.add(GraphNode.of(key, null))
                    }
                }
            }
        }
    }

    private fun extractGraph(graphString: String, key: String, map: Graph): Pair<String, Graph> {
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