package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.schema.ScalarSupport
import kotlin.reflect.KClass
import kotlin.reflect.KFunction


sealed class KQLObject(val name : String) {

    class Mutation<T>(name : String, val wrapper: FunctionWrapper<T>) : KQLObject(name), FunctionWrapper<T>{

        override fun invoke(vararg args: Any?): T? = wrapper.invoke(*args)

        override val kFunction: KFunction<T> = wrapper.kFunction

        override fun arity(): Int = wrapper.arity()
    }

    class Query<T>(name: String, val wrapper: FunctionWrapper<T>) : KQLObject(name), FunctionWrapper<T>{

        override fun invoke(vararg args: Any?): T? = wrapper.invoke(*args)

        override val kFunction: KFunction<T> = wrapper.kFunction

        override fun arity(): Int = wrapper.arity()
    }

    class Simple<T : Any>(name : String, val kClass: KClass<T>) : KQLObject(name)

    class Scalar<T : Any>(
            name : String,
            val kClass: KClass<T>,
            val scalarSupport : ScalarSupport<T>
    ) : KQLObject(name)

    class Input<T : Any>(name : String, val kClass: KClass<T>) : KQLObject(name)

    class Interface<T : Any>(name : String, val kClass: KClass<T>) : KQLObject(name)
}