package com.github.pgutkowski.kgraphql.schema.dsl

import kotlin.reflect.KClass


class InputValuesDSL(block : InputValuesDSL.() -> Unit) {

    val inputValues = mutableListOf<InputValueDSL<*>>()

    init {
        block()
    }

    fun <T : Any> arg(kClass: KClass<T>, block : InputValueDSL<T>.() -> Unit){
        inputValues.add(InputValueDSL(kClass, block))
    }

    inline fun <reified T : Any> arg(noinline block : InputValueDSL<T>.() -> Unit){
        arg(T::class, block)
    }

}