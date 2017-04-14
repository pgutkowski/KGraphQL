package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * KQLObject represents part of KGraphQL schema. Subclasses mirror schema elements stated in GraphQL spec.
 * KQLObject's name should be unique in scope of schema
 */
sealed class KQLObject(val name : String) {

    class Mutation<T>(name : String, private val wrapper: FunctionWrapper<T>) : KQLObject(name), FunctionWrapper<T>{

        override fun invoke(vararg args: Any?): T? = wrapper.invoke(*args)

        override val kFunction: KFunction<T> = wrapper.kFunction

        override fun arity(): Int = wrapper.arity()
    }

    class Query<T>(name: String, private val wrapper: FunctionWrapper<T>) : KQLObject(name), FunctionWrapper<T>{

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

    class Interface<T : Any>(name : String, val kClass: KClass<T>) : KQLObject(name)

    class Enumeration<T : Enum<T>>(name: String, val kClass: KClass<T>, val values: Array<T>) : KQLObject(name)
}