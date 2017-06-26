package com.github.pgutkowski.kgraphql.schema.introspection

import com.github.pgutkowski.kgraphql.schema.directive.DirectiveLocation


data class __Directive(
        override val name: String,
        override val description: String?,
        val locations : List<DirectiveLocation>,
        val args: List<__InputValue>
) : __Described