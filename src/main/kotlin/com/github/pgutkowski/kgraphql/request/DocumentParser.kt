package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.graph.*

/**
 * Utility for parsing query document and its structures.
 * TODO: too complex, has to refactor this
 */
class DocumentParser {

    private data class ParsingContext(
            val fullString : String,
            val tokens: List<String>,
            val fragments: Map<String, Fragment.External>,
            var index : Int = 0,
            var nestedBrackets: Int = 0,
            var nestedParenthesis: Int = 0
    ) {
        operator fun get(index: Int): String = tokens[index]

        fun currentToken() = tokens[index]

        fun currentTokenOrNull() = tokens.getOrNull(index)

        fun followingTokenOrNull() = tokens.getOrNull(index + 1)

        fun getFullStringIndex(tokenIndex : Int = index) : Int {
            //TODO: Provide reliable index
            return 0
        }
    }

    /**
     * Performs validation and parsing of query document, returning all declared operations.
     * Fragments declared in document are parsed as well, but only used to create operations and not persisted.
     */
    fun parseDocument(input: String) : List<Operation> {
        val request = validateAndFilterRequest(input)
        val documentTokens = createDocumentTokens(tokenizeRequest(request))
        val fragments = mutableMapOf<String, Fragment.External>()

        documentTokens.fragmentsTokens.forEach { (name, typeCondition, graphTokens) ->
            val fragmentGraph = parseGraph(ParsingContext(input, graphTokens, fragments))
            fragments.put("...$name", Fragment.External("...$name", fragmentGraph, typeCondition))
        }

        return documentTokens.operationTokens.map { (name, type, graphTokens) ->
            Operation(parseGraph(ParsingContext(input, graphTokens, fragments)), name, Operation.Action.parse(type))
        }
    }

    /**
     * @param tokens - should be list of valid GraphQL tokens
     */
    fun parseGraph(input: String, fragments: Map<String, Fragment.External> = emptyMap()): Graph {
        return parseGraph(ParsingContext(input, tokenizeRequest(input), fragments))
    }

    /**
     * @param tokens - should be list of valid GraphQL tokens
     */
    private fun parseGraph(input: String, tokens: List<String>, fragments: Map<String, Fragment.External> = emptyMap()): Graph {
        return parseGraph(ParsingContext(input, tokens, fragments))
    }

    private fun parseGraph(ctx : ParsingContext) : Graph {
        val graph = GraphBuilder()
        while (ctx.index < ctx.tokens.size) {
            val token = ctx.currentToken()
            if(token in OPERANDS){
                handleOperands(token, ctx)
            } else {
                val (alias, key) = extractAliasAndKey(ctx)
                val directives : List<DirectiveInvocation>? = parseDirectives(ctx)
                when (ctx.followingTokenOrNull()) {
                    "{" -> graph.add(parseNode(ctx, key, alias, directives))
                    "(" -> graph.add(parseNodeWithArguments(ctx, key, alias, directives))
                    else -> {
                        if(key.startsWith("...")){
                            if(key == "..."){
                                graph.add(parseInlineFragment(ctx, directives))
                            } else {
                                graph.add(ctx.fragments[key] ?: throw SyntaxException("Fragment $key} does not exist"))
                            }
                        } else {
                            graph.add(GraphNode(key = key, alias = alias, directives = directives))
                        }
                    }
                }
            }
            ctx.index++
        }

        when {
            ctx.nestedBrackets != 0 -> throw SyntaxException("Missing closing bracket")
            ctx.nestedParenthesis != 0 -> throw SyntaxException("Missing closing parenthesis")
        }

        return graph.build()
    }

    private fun extractAliasAndKey(ctx: ParsingContext): Pair<String?, String> {
        val token = ctx[ctx.index]
        return if (ctx.tokens.size > ctx.index + 1 && ctx[ctx.index + 1] == ":") {
            ctx.index += 2
            token to ctx[ctx.index]
        } else {
            null to token
        }
    }

    /**
     * Assumption is that directive is always 'on' some token, so this method starts from following token, not current
     */
    private fun parseDirectives(ctx: ParsingContext) : List<DirectiveInvocation>? {
        val directives = arrayListOf<DirectiveInvocation>()
        var nextDirective : DirectiveInvocation? = parseDirective(ctx, true)
        if(nextDirective != null){
            while(nextDirective != null){
                ctx.index++
                directives.add(nextDirective)
                nextDirective = parseDirective(ctx)
            }
            return directives
        } else {
            return null
        }
    }

    private fun parseDirective(ctx: ParsingContext, following : Boolean = false) : DirectiveInvocation? {
        val directiveName = if(following) ctx.followingTokenOrNull() else ctx.currentTokenOrNull()
        if(directiveName != null && directiveName.startsWith("@")){
            if(following) ctx.index++
            if(ctx.followingTokenOrNull() == "("){
                val arguments = parseArguments(ctx, ctx.index+1)
                return (DirectiveInvocation(directiveName, arguments))
            } else {
                return (DirectiveInvocation(directiveName))
            }
        } else {
            return null
        }
    }

    private fun parseInlineFragment(ctx: ParsingContext, directives: List<DirectiveInvocation>?): Fragment.Inline {
        if (ctx.followingTokenOrNull() != "on") {
            throw SyntaxException("expected 'on #typeCondition {selection set}' after '...' in inline fragment")
        }

        val typeCondition = ctx[ctx.index + 2]
        val subGraphTokens = objectSubTokens(ctx.tokens, ctx.index + 3)
        ctx.index += (subGraphTokens.size + 4)
        return Fragment.Inline(parseGraph(ctx.fullString, subGraphTokens, ctx.fragments), typeCondition, directives)
    }

    private fun parseNodeWithArguments(ctx: ParsingContext, key: String, alias: String?, directives: List<DirectiveInvocation>?): GraphNode {
        val arguments = parseArguments(ctx, ctx.index + 1)

        var subGraph: Graph? = null
        if (ctx.tokens.getOrNull(ctx.index + 1) == "{") {
            val subGraphTokens = objectSubTokens(ctx.tokens, ctx.index + 1)
            subGraph = parseGraph(ctx.fullString, subGraphTokens, ctx.fragments)
            ctx.index += subGraphTokens.size + 2 //subtokens do not contain '{' and '}'
        }
        return GraphNode(key, alias, subGraph, arguments, directives)
    }

    private fun parseArguments(ctx: ParsingContext, startIndex: Int): Arguments {
        val argTokens = argsSubTokens(ctx.tokens, startIndex)
        val arguments = Arguments()
        var i = 0
        while (i + 2 < argTokens.size) {
            when{
                argTokens[i+1] == ":" && argTokens[i + 2] == "[" -> {
                    val argumentName = argTokens[i]
                    i += 2 // effectively 'i' is index of '['
                    val deltaOfClosingBracket = argTokens.subList(i, argTokens.size).indexOfFirst { it == "]" }
                    if(deltaOfClosingBracket == -1) throw SyntaxException("Missing closing ']' in arguments ${argTokens.joinToString(" ")}")
                    val indexOfClosingBracket = i + deltaOfClosingBracket
                    //exclude '[' and ']'
                    arguments.put(argumentName, argTokens.subList(i + 1, indexOfClosingBracket))
                    i += deltaOfClosingBracket + 1
                }
                argTokens[i+1] == ":" ->{
                    arguments.put(argTokens[i], argTokens[i + 2])
                    i += 3
                }
                else -> {
                    throw SyntaxException("Invalid arguments: ${argTokens.joinToString(" ")}")
                }
            }
        }
        ctx.index += argTokens.size + 2 //subtokens do not contain '(' and ')'
        return arguments
    }

    private fun parseNode(ctx: ParsingContext, key: String, alias: String?, directives: List<DirectiveInvocation>?): GraphNode {
        val subGraphTokens = objectSubTokens(ctx.tokens, ctx.index + 1)
        ctx.index += subGraphTokens.size + 2 //subtokens do not contain '{' and '}'
        return GraphNode(key, alias, parseGraph(ctx.fullString, subGraphTokens, ctx.fragments), null, directives)
    }

    private fun handleOperands(token: String, ctx: ParsingContext) {
        when (token) {
            "{" -> ctx.nestedBrackets++
            "}" -> ctx.nestedBrackets--
            "(" -> ctx.nestedParenthesis++
            ")" -> ctx.nestedParenthesis--
            "[","]" -> throw SyntaxException("Unexpected token : $token at ${ctx.getFullStringIndex()}")
        }
        if (ctx.nestedBrackets < 0) {
            throw SyntaxException("No matching opening bracket for closing bracket at ${ctx.getFullStringIndex()}")
        }
        if (ctx.nestedParenthesis < 0) {
            throw SyntaxException("No matching opening parenthesis for closing parenthesis at ${ctx.getFullStringIndex()}")
        }
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
}