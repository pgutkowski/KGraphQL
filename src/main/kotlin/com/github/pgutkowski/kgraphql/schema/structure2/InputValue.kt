package com.github.pgutkowski.kgraphql.schema.structure2

import com.github.pgutkowski.kgraphql.schema.introspection.__InputValue
import com.github.pgutkowski.kgraphql.schema.model.InputValueDef


class InputValue<T : Any>(
        valueDef: InputValueDef<T>,
        override val type: Type,
        //TODO: Replace with GraphQL compliant representation
        override val defaultValue: String? = valueDef.defaultValue?.toString()
) : __InputValue {

    override val name: String = valueDef.name

    override val description: String? = valueDef.description

    val default: T? = valueDef.defaultValue
}