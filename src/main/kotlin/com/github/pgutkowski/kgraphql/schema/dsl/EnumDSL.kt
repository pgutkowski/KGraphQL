package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.defaultKQLTypeName
import kotlin.reflect.KClass


class EnumDSL<T : Enum<T>>(kClass: KClass<T>, block : (EnumDSL<T>.() -> Unit)?) : ItemDSL() {

    override var name = kClass.defaultKQLTypeName()

    init {
        block?.invoke(this)
    }
}