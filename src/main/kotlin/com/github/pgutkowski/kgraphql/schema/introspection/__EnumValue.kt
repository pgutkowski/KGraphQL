package com.github.pgutkowski.kgraphql.schema.introspection

import com.github.pgutkowski.kgraphql.schema.model.Depreciable


class __EnumValue(
        override val name: String,
        override val description: String? = null,
        override val isDeprecated: Boolean = false,
        override val deprecationReason: String? = null
) : Depreciable, Describable