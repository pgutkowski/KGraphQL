package com.github.pgutkowski.kgraphql.request

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.request.graph.*

/**
 * Utility for parsing query document and its structures.
 * TODO: too complex, has to refactor this
 */
open class DocumentParser {

    /**
     * Performs validation and parsing of query document, returning all declared operations.
     * Fragments declared in document are parsed as well, but only used to create operations and not persisted.
     */
    open fun parseDocument(input: String) : List<Operation> {
        val request = validateAndFilterRequest(input)
        val documentTokens = createDocumentTokens(tokenizeRequest(request))
        val fragments = mutableMapOf<String, Fragment.External>()

        documentTokens.fragmentsTokens.forEach { (name, typeCondition, graphTokens) ->
            val fragmentGraph = parseSelectionTree(ParsingContext(input, graphTokens, fragments))
            fragments.put("...$name", Fragment.External("...$name", fragmentGraph, typeCondition))
        }

        return documentTokens.operationTokens.map { (name, type, operationVariables, graphTokens) ->
            Operation (
                    selectionTree = parseSelectionTree(ParsingContext(input, graphTokens, fragments)),
                    variables = operationVariables,
                    name = name,
                    action = Operation.Action.parse(type)
            )
        }
    }

    fun parseSelectionTree(input: String, fragments: Map<String, Fragment.External> = emptyMap()): SelectionTree {
        return parseSelectionTree(ParsingContext(input, tokenizeRequest(input), fragments))
    }

    private fun parseSelectionTree(input: String, tokens: List<String>, fragments: Map<String, Fragment.External> = emptyMap()): SelectionTree {
        return parseSelectionTree(ParsingContext(input, tokens, fragments))
    }

    private fun parseSelectionTree(ctx : ParsingContext) : SelectionTree {
        val graph = SelectionSetBuilder()
        while (ctx.index() < ctx.tokens.size) {
            val token = ctx.currentToken()
            if(token in OPERANDS){
                handleOperands(token, ctx)
            } else {
                handleNode(ctx, graph)
            }
            ctx.next()
        }

        when {
            ctx.nestedBrackets != 0 -> throw SyntaxException("Missing closing bracket")
            ctx.nestedParenthesis != 0 -> throw SyntaxException("Missing closing parenthesis")
        }

        return graph.build()
    }

    private fun handleNode(ctx: ParsingContext, graph: SelectionSetBuilder) {
        val (alias, key) = extractAliasAndKey(ctx)
        val directives: List<DirectiveInvocation>? = parseDirectives(ctx)
        when (ctx.peekToken()) {
            "{" -> graph.add(parseNode(ctx, key, alias, directives))
            "(" -> graph.add(parseNodeWithArguments(ctx, key, alias, directives))
            else -> {
                if (key.startsWith("...")) {
                    if (key == "...") {
                        graph.add(parseInlineFragment(ctx, directives))
                    } else {
                        graph.add(ctx.fragments[key] ?: throw SyntaxException("Fragment $key} does not exist"))
                    }
                } else {
                    graph.add(SelectionNode(key = key, alias = alias, directives = directives))
                }
            }
        }
    }

    private fun extractAliasAndKey(ctx: ParsingContext): Pair<String?, String> {
        val token = ctx.currentToken()
        return if (ctx.tokens.size > ctx.index() + 1 && ctx.peekToken() == ":") {
            ctx.next(2)
            token to ctx.currentToken()
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
                ctx.next()
                directives.add(nextDirective)
                nextDirective = parseDirective(ctx)
            }
            return directives
        } else {
            return null
        }
    }

    private fun parseDirective(ctx: ParsingContext, following : Boolean = false) : DirectiveInvocation? {
        val directiveName = if(following) ctx.peekToken() else ctx.currentTokenOrNull()
        if(directiveName != null && directiveName.startsWith("@")){
            if(following) ctx.next()
            if(ctx.peekToken() == "("){
                ctx.next(1)
                val arguments = parseArguments(ctx)
                return (DirectiveInvocation(directiveName, arguments))
            } else {
                return (DirectiveInvocation(directiveName))
            }
        } else {
            return null
        }
    }

    private fun parseInlineFragment(ctx: ParsingContext, directives: List<DirectiveInvocation>?): Fragment.Inline {
        when{
            ctx.peekToken() == "on" -> {
                val typeCondition = ctx.peekToken(2)
                ctx.next(3)
                val subGraphTokens = ctx.traverseObject()
                return Fragment.Inline(parseSelectionTree(ctx.fullString, subGraphTokens, ctx.fragments), typeCondition, null)
            }
            ctx.currentToken() == "{" && directives?.isNotEmpty() ?: false -> {
                val subGraphTokens = ctx.traverseObject()
                return Fragment.Inline(parseSelectionTree(ctx.fullString, subGraphTokens, ctx.fragments), null, directives)
            }
            else -> throw SyntaxException("expected type condition or directive after '...' in inline fragment")
        }
    }

    private fun parseNodeWithArguments(ctx: ParsingContext, key: String, alias: String?, directives: List<DirectiveInvocation>?): SelectionNode {
        ctx.next()
        val arguments = parseArguments(ctx)

        var subGraph: SelectionTree? = null
        if (ctx.peekToken(1) == "{") {
            ctx.next()
            val subGraphTokens = ctx.traverseObject()
            subGraph = parseSelectionTree(ctx.fullString, subGraphTokens, ctx.fragments)
        }
        return SelectionNode(key, alias, subGraph, arguments, directives)
    }

    private fun parseArguments(ctx: ParsingContext): Arguments {
        val argTokens = ctx.traverseArguments()
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
                argTokens[i+1] == ":" && argTokens[i + 2] == "{" -> {
                    throw UnsupportedOperationException("Object literal arguments are not supported yet")
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
        return arguments
    }

    private fun parseNode(ctx: ParsingContext, key: String, alias: String?, directives: List<DirectiveInvocation>?): SelectionNode {
        ctx.next()
        val subGraphTokens = ctx.traverseObject()
        return SelectionNode(key, alias, parseSelectionTree(ctx.fullString, subGraphTokens, ctx.fragments), null, directives)
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
}