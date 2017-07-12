package com.github.pgutkowski.kgraphql.schema.model

import kotlin.reflect.KClass


class InputValueDef<T : Any>(
        val kClass : KClass<T>,
        val name : String,
        val defaultValue : T? = null,
        override val isDeprecated: Boolean = false,
        override val description: String? = null,
        override val deprecationReason: String? = null
) : DescribedDef, Depreciable