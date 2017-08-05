package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.model.InputValueDef
import kotlin.reflect.KClass


class InputValueDSL<T : Any>(val kClass: KClass<T>, block : InputValueDSL<T>.() -> Unit) : DepreciableItemDSL() {

    init {
        block()
    }

    lateinit var name : String

    var defaultValue : T? = null

    fun toKQLInputValue() : InputValueDef<T> = InputValueDef(
            kClass = kClass,
            name = name,
            defaultValue = defaultValue,
            isDeprecated = isDeprecated,
            description = description,
            deprecationReason = deprecationReason
    )
}