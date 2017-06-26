package com.github.pgutkowski.kgraphql.schema.introspection

import com.github.pgutkowski.kgraphql.schema.model.Depreciable


data class __Field (
        override val name: String,
        override val description: String?,
        val type: __Type,
        val args: List<__InputValue>,
        override val isDeprecated: Boolean,
        override val deprecationReason: String?
) : Depreciable, __Described