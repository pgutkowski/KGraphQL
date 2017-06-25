package com.github.pgutkowski.kgraphql.schema.model


class KQLEnumValue<T : Enum<T>>(
        val value: T,
        override val description: String? = null,
        override val isDeprecated: Boolean = false,
        override val deprecationReason: String? = null
) : DescribedKQLObject, Depreciable {
    val name = value.name
}