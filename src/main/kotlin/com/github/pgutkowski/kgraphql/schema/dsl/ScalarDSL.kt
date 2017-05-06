package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import kotlin.reflect.KClass


class ScalarDSL<T : Any>(kClass: KClass<T>, block: ScalarDSL<T>.() -> Unit) : TypeDSL<T>(kClass, null){

    init {
        block()
    }

    var support : ScalarSupport<T>? = null

    var serialize : ((String) -> T)? = null

    var deserialize : ((T) -> String)? = null

    var validate : ((String)-> Boolean)? = null
}