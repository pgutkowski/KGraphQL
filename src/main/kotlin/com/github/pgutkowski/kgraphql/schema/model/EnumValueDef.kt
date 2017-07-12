package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.schema.structure2.EnumValue


class EnumValueDef<T : Enum<T>>(
        val value: T,
        override val description: String? = null,
        override val isDeprecated: Boolean = false,
        override val deprecationReason: String? = null
) : DescribedDef, Depreciable {
    val name = value.name

    fun toEnumValue (): EnumValue<T> {
        return EnumValue(this)
    }
}