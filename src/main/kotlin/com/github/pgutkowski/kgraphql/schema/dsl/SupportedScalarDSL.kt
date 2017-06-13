package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import com.github.pgutkowski.kgraphql.defaultKQLTypeName
import kotlin.reflect.KClass


class SupportedScalarDSL<T : Any>(kClass: KClass<T>, block: SupportedScalarDSL<T>.() -> Unit) : ItemDSL() {

    override var name = kClass.defaultKQLTypeName()

    init {
        block()
    }

    var support : ScalarSupport<T>? = null
}