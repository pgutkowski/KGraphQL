package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.graph.Fragment
import com.github.pgutkowski.kgraphql.graph.Graph
import com.github.pgutkowski.kgraphql.graph.GraphBuilder
import com.github.pgutkowski.kgraphql.graph.GraphNode


class GraphParser {

    fun parse(input: String): Graph {
        validateInput(input)
        val tokens = tokenizeRequest(input)
        val (fragmentTokens, graphTokens) = split(tokens.filter(String::isNotBlank))
        val fragments = buildFragments(fragmentTokens)
        return buildGraph(graphTokens, fragments)
    }

    private fun buildFragments(tokens : List<String>) : Map<String, Fragment.External> {
        val fragments : MutableMap<String, Fragment.External> = mutableMapOf()
        var index = 0
        var typeCondition : String? = null
        var key : String = ""

        while (index < tokens.size) {
            val token = tokens[index]
            when(token) {
                "fragment" -> {/* DO NOTHING*/}
                "on" -> {
                    typeCondition = tokens[index + 1]
                    index += 1
                }
                "{" -> {
                    val subTokens = objectSubTokens(tokens, index)
                    if(key.isBlank()) throw SyntaxException("Invalid fragment without name")
                    if(fragments.any { it.key == key }) throw SyntaxException("Duplicated fragments with name $key")

                    val fragmentKey = "...$key"
                    fragments.put(fragmentKey, Fragment.External(fragmentKey, buildGraph(subTokens, fragments), typeCondition))
                    index += subTokens.size + 2
                    typeCondition = null
                    key = ""
                }
                else -> key = token
            }
            index ++
        }

        return fragments
    }

    private fun buildGraph(tokens: List<String>, fragments: Map<String, Fragment.External>): Graph {
        val graph = GraphBuilder()
        var index = 0
        var nestedBrackets = 0
        var nestedParenthesis = 0

        while (index < tokens.size) {
            val token = tokens[index]
            if(token in OPERANDS){
                when(token){
                    "{" -> nestedBrackets++
                    "}" -> {
                        nestedBrackets--
                        if(nestedBrackets < 0){
                            throw SyntaxException("No matching opening bracket for closing bracket at ${getIndexOfTokenInString(tokens)}")
                        }
                    }
                    "(" -> nestedParenthesis++
                    ")" -> {
                        nestedParenthesis--
                        if(nestedParenthesis < 0){
                            throw SyntaxException("No matching opening parenthesis for closing parenthesis at ${getIndexOfTokenInString(tokens)}")
                        }
                    }
                }
            } else {
                val (alias, key) = if (tokens.size > index + 1 && tokens[index + 1] == ":") {
                    index += 2
                    token to tokens[index]
                } else {
                    null to token
                }

                when (tokens.getOrNull(index + 1)) {
                    "{" -> {
                        val subGraphTokens = objectSubTokens(tokens, index + 1)
                        validateAndAdd(graph, GraphNode(key, alias, buildGraph(subGraphTokens, fragments)))
                        index += subGraphTokens.size + 2 //subtokens do not contain '{' and '}'
                    }
                    "(" -> {
                        val argTokens = argsSubTokens(tokens, index + 1)
                        val arguments = buildArguments(argTokens)
                        index += argTokens.size + 2 //subtokens do not contain '(' and ')'

                        var subGraph: Graph? = null
                        if (tokens.getOrNull(index + 1) == "{") {
                            val subGraphTokens = objectSubTokens(tokens, index + 1)
                            subGraph = buildGraph(subGraphTokens, fragments)
                            index += subGraphTokens.size + 2 //subtokens do not contain '{' and '}'
                        }
                        validateAndAdd(graph, GraphNode(key, alias, subGraph, arguments))
                    }
                    else -> {
                        if(key.startsWith("...")){
                            if(key == "..."){
                                if(tokens.getOrNull(index + 1) != "on"){
                                    throw SyntaxException("expected 'on #typeCondition {selection set}' after '...' in inline fragment")
                                }

                                val typeCondition = tokens[index + 2]
                                val subGraphTokens = objectSubTokens(tokens, index + 3)
                                validateAndAdd(graph, Fragment.Inline(buildGraph(subGraphTokens, fragments), typeCondition))
                                index += (subGraphTokens.size + 4)
                            } else {
                                val fragment = fragments[key] ?: throw SyntaxException("Fragment $key} does not exist")
                                validateAndAdd(graph, fragment)
                            }
                        } else {
                            validateAndAdd(graph, GraphNode(key, alias))
                        }
                    }
                }
            }
            ++index
        }

        when {
            nestedBrackets != 0 -> throw SyntaxException("Missing closing bracket")
            nestedParenthesis != 0 -> throw SyntaxException("Missing closing parenthesis")
        }

        return graph.build()
    }

    fun buildArguments(tokens: List<String>): Arguments {
        val arguments = Arguments()
        var i = 0
        while (i + 2 < tokens.size) {
            assert(tokens[i + 1] == ":")
            arguments.put(tokens[i], tokens[i + 2])
            i += 3
        }
        return arguments
    }

    fun objectSubTokens(allTokens: List<String>, startIndex: Int) = subTokens(allTokens, startIndex, "{", "}")

    fun argsSubTokens(allTokens: List<String>, startIndex: Int) = subTokens(allTokens, startIndex, "(", ")")

    fun subTokens(allTokens: List<String>, startIndex: Int, openingToken: String, closingToken: String): List<String> {
        val tokens = allTokens.subList(startIndex, allTokens.size)
        var nestedLevel = 0
        tokens.forEachIndexed { index, token ->
            when (token) {
                openingToken -> nestedLevel++
                closingToken -> nestedLevel--
            }
            if (nestedLevel == 0) return tokens.subList(1, index)
        }
        throw SyntaxException("Couldn't find matching closing token")
    }

    private fun validateAndAdd(graph: GraphBuilder, node: GraphNode) {
        when {
            node.key.isBlank() -> throw SyntaxException("cannot handle blank property in object : $graph")
            graph.any { it.aliasOrKey == node.aliasOrKey } -> throw SyntaxException("Duplicated property name/alias: ${node.aliasOrKey}")
            else -> graph.add(node)
        }
    }

    private fun validateInput(string: String) {
        val trimmedString = string.trim()
        if (!trimmedString.startsWith("{") || !trimmedString.endsWith("}")) {
            throw SyntaxException("passed string $string does not represent valid query document")
        }
    }
}