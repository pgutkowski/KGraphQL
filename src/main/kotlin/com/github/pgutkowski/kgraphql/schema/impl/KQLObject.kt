package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * KQLObject represents part of KGraphQL schema. Subclasses represent schema elements stated in GraphQL spec.
 * KQLObject's name should be unique in scope of schema
 */
sealed class KQLObject(val name : String, val description : String?) {

    class Mutation<T>(name : String, private val wrapper: FunctionWrapper<T>, description : String?) : KQLObject(name, description), FunctionWrapper<T>{

        override fun invoke(vararg args: Any?): T? = wrapper.invoke(*args)

        override val kFunction: KFunction<T> = wrapper.kFunction

        override fun arity(): Int = wrapper.arity()
    }

    class Query<T>(name: String, private val wrapper: FunctionWrapper<T>, description : String?) : KQLObject(name, description), FunctionWrapper<T>{

        override fun invoke(vararg args: Any?): T? = wrapper.invoke(*args)

        override val kFunction: KFunction<T> = wrapper.kFunction

        override fun arity(): Int = wrapper.arity()
    }

    class Simple<T : Any>(name : String, val kClass: KClass<T>, description : String?) : KQLObject(name, description)

    class Scalar<T : Any>(
            name : String,
            val kClass: KClass<T>,
            val scalarSupport : ScalarSupport<T>,
            description : String?
    ) : KQLObject(name, description)

    class Interface<T : Any>(
            name : String,
            val kClass: KClass<T>,
            description : String?
    ) : KQLObject(name, description)

    class Enumeration<T : Enum<T>>(
            name: String,
            val kClass: KClass<T>,
            val values: Array<T>,
            description : String?
    ) : KQLObject(name, description)
}