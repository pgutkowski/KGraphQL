package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.graph.Fragment
import com.github.pgutkowski.kgraphql.graph.Graph
import com.github.pgutkowski.kgraphql.graph.GraphBuilder
import com.github.pgutkowski.kgraphql.graph.GraphNode

/**
 * Utility for parsing query document and its structures.
 */
class DocumentParser {

    /**
     * Performs validation and parsing of query document, returning all declared operations.
     * Fragments declared in document are parsed as well, but only used to create operations and not persisted.
     */
    fun parseDocument(input: String) : List<Operation> {
        val request = validateAndFilterRequest(input)
        val documentTokens = createDocumentTokens(tokenizeRequest(request))
        val fragments = mutableMapOf<String, Fragment.External>()

        documentTokens.fragmentsTokens.forEach { (name, typeCondition, graphTokens) ->
            fragments.put("...$name", Fragment.External("...$name", parseGraph(graphTokens, fragments), typeCondition))
        }

        return documentTokens.operationTokens.map { (name, type, graphTokens) ->
            Operation(parseGraph(graphTokens, fragments), name, Operation.Action.parse(type))
        }
    }

    /**
     * @param tokens - should be list of valid GraphQL tokens
     */
    fun parseGraph(tokens: List<String>, fragments: Map<String, Fragment.External> = emptyMap()): Graph {
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
                        validateAndAdd(graph, GraphNode(key, alias, parseGraph(subGraphTokens, fragments)))
                        index += subGraphTokens.size + 2 //subtokens do not contain '{' and '}'
                    }
                    "(" -> {
                        val argTokens = argsSubTokens(tokens, index + 1)
                        val arguments = parseArguments(argTokens)
                        index += argTokens.size + 2 //subtokens do not contain '(' and ')'

                        var subGraph: Graph? = null
                        if (tokens.getOrNull(index + 1) == "{") {
                            val subGraphTokens = objectSubTokens(tokens, index + 1)
                            subGraph = parseGraph(subGraphTokens, fragments)
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
                                validateAndAdd(graph, Fragment.Inline(parseGraph(subGraphTokens, fragments), typeCondition))
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

    fun parseArguments(tokens: List<String>): Arguments {
        val arguments = Arguments()
        var i = 0
        while (i + 2 < tokens.size) {
            assert(tokens[i + 1] == ":")
            arguments.put(tokens[i], tokens[i + 2])
            i += 3
        }
        return arguments
    }

    private fun objectSubTokens(allTokens: List<String>, startIndex: Int) = subTokens(allTokens, startIndex, "{", "}")

    private fun argsSubTokens(allTokens: List<String>, startIndex: Int) = subTokens(allTokens, startIndex, "(", ")")

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
        throw SyntaxException("Couldn't find matching closing token")
    }

    private fun validateAndAdd(graph: GraphBuilder, node: GraphNode) {
        when {
            node.key.isBlank() -> throw SyntaxException("cannot handle blank property in object : $graph")
            graph.any { it.aliasOrKey == node.aliasOrKey } -> throw SyntaxException("Duplicated property name/alias: ${node.aliasOrKey}")
            else -> graph.add(node)
        }
    }
}