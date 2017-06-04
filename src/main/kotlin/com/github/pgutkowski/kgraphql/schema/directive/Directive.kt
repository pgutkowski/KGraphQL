package com.github.pgutkowski.kgraphql.schema.directive

import com.github.pgutkowski.kgraphql.schema.directive.DirectiveLocation.*
import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper

/**
 * Directives provide a way to describe alternate runtime execution and type validation behavior in a GraphQL document.
 */
data class Directive(val name: String, val locations: Set<DirectiveLocation>, val execution: DirectiveExecution){
    companion object {
        /**
         * The @skip directive may be provided for fields, fragment spreads, and inline fragments.
         * Allows for conditional exclusion during execution as described by the if argument.
         */
        val SKIP = Directive( "skip",
                setOf(FIELD, FRAGMENT_SPREAD, INLINE_FRAGMENT),
                DirectiveExecution(FunctionWrapper.on({ `if` : Boolean -> DirectiveResult(!`if`) }))
        )

        /**
         * The @include directive may be provided for fields, fragment spreads, and inline fragments.
         * Allows for conditional inclusion during execution as described by the if argument.
         */
        val INCLUDE = Directive( "include",
                setOf(FIELD, FRAGMENT_SPREAD, INLINE_FRAGMENT),
                DirectiveExecution(FunctionWrapper.on({ `if` : Boolean -> DirectiveResult(`if`) }))
        )
    }
}