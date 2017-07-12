package com.github.pgutkowski.kgraphql.schema.structure2

import com.github.pgutkowski.kgraphql.schema.introspection.__EnumValue
import com.github.pgutkowski.kgraphql.schema.model.EnumValueDef


class EnumValue<T : Enum<T>>(definition:  EnumValueDef<T>) : __EnumValue {

    val value = definition.value

    override val isDeprecated: Boolean = definition.isDeprecated

    override val name: String = definition.name

    override val description: String? = definition.description

    override val deprecationReason: String? = definition.deprecationReason
}