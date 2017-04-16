package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.graph.Graph
import com.github.pgutkowski.kgraphql.graph.GraphNode
import java.util.*


class GraphParser {
    companion object {
        private val DELIMITERS = "{}\n,()"
    }

    fun parse(input: String): Graph {
        validateInput(input)
        val tokens = StringTokenizer(input.dropBrackets(), DELIMITERS, true).toList().map { (it as String) }
        return buildGraph(tokens)
    }

    private fun buildGraph(tokens: List<String>) : Graph {
        val graph = Graph()
        var index = 0
        while(index < tokens.size){
            val token = tokens[index]
            if (token !in DELIMITERS) {
                val (alias, key) = splitAliasAndKey(token.trim())
                when (tokens.getOrNull(index + 1)) {
                    "{" -> {
                        val subGraphTokens = objectSubTokens(tokens, index+1)
                        validateAndAdd(graph, GraphNode(key, alias, buildGraph(subGraphTokens)))
                        index += subGraphTokens.size + 2 //subtokens do not contain '{' and '}'
                    }
                    "(" -> {
                        val argTokens = argsSubTokens(tokens, index+1)
                        val arguments = buildArguments(argTokens)
                        index += argTokens.size + 2 //subtokens do not contain '(' and ')'

                        var subGraph : Graph? = null
                        if(tokens.getOrNull(index + 1) == "{"){
                            val subGraphTokens = objectSubTokens(tokens, index+1)
                            subGraph = buildGraph(subGraphTokens)
                            index += subGraphTokens.size + 2 //subtokens do not contain '{' and '}'
                        }
                        validateAndAdd(graph, GraphNode(key, alias, subGraph, arguments))
                    }
                    "}" -> {
                        throw SyntaxException("No matching opening bracket for closing bracket at ${getIndexOfTokenInString(tokens)}")
                    }
                    ")" -> {
                        throw SyntaxException("No matching opening parenthesis for closing parenthesis at ${getIndexOfTokenInString(tokens)}")
                    }
                    else -> {
                        validateAndAdd(graph, GraphNode(key, alias))
                    }
                }
            }
            ++index
        }

        return graph
    }

    fun buildArguments(tokens: List<String>) : Arguments {
        val arguments = Arguments()
        tokens.filterNot { it in DELIMITERS }.forEach { token ->
            val splitToken = token.split(':')
            arguments.put(splitToken[0].trim(), splitToken[1].trim())
        }
        return arguments
    }

    fun objectSubTokens(allTokens: List<String>, startIndex: Int) = subTokens(allTokens, startIndex, "{", "}")

    fun argsSubTokens(allTokens: List<String>, startIndex: Int) = subTokens(allTokens, startIndex, "(", ")")

    fun subTokens(allTokens: List<String>, startIndex: Int, openingToken : String, closingToken: String) : List<String> {
        val tokens = allTokens.subList(startIndex, allTokens.size)
        var nestedLevel = 0
        tokens.forEachIndexed { index, token ->
            when(token){
                openingToken -> nestedLevel++
                closingToken -> nestedLevel--
            }
            if(nestedLevel == 0) return tokens.subList(1, index)
        }
        throw SyntaxException("Couldn't find matching closing token")
    }

    private fun getIndexOfTokenInString(tokens: List<String>) : Int{
        return tokens.fold(0, {index, token -> index + token.length })
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

    private fun validateAndAdd(graph: Graph, node: GraphNode){
        when{
            node.key.isBlank() -> throw SyntaxException("cannot handle blank property in object : $graph")
            graph.any { it.aliasOrKey == node.aliasOrKey } -> throw SyntaxException("Duplicated property name/alias: ${node.aliasOrKey}")
            else -> graph.add(node)
        }
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
}