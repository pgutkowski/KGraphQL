package com.github.pgutkowski.kgraphql.schema.introspection

import com.github.pgutkowski.kgraphql.schema.directive.DirectiveLocation


interface __Directive : Describable {

    val locations : List<DirectiveLocation>

    val args: List<__InputValue>
}