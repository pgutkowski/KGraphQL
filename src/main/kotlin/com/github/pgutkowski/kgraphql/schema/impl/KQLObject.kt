package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

/**
 * KQLObject represents part of KGraphQL schema. Subclasses represent schema elements stated in GraphQL spec.
 * KQLObject's name should be unique in scope of schema
 */
sealed class KQLObject(val name : String, val description : String?) {

    abstract class Operation<T>(
            name : String,
            private val wrapper: FunctionWrapper<T>,
            description : String?
    ) : KQLObject(name, description), FunctionWrapper<T> {

        override fun invoke(vararg args: Any?): T? = wrapper.invoke(*args)

        override val kFunction: KFunction<T> = wrapper.kFunction

        override fun arity(): Int = wrapper.arity()
    }

    class Mutation<T> (
            name : String,
            wrapper: FunctionWrapper<T>,
            description : String?
    ) : KQLObject.Operation<T>(name, wrapper, description)

    class Query<T> (
            name : String,
            wrapper: FunctionWrapper<T>,
            description : String?
    ) : KQLObject.Operation<T>(name, wrapper, description)

    class Object<T : Any> (
            name : String,
            val kClass: KClass<T>,
            val ignoredProperties : List<KProperty<*>>,
            description : String?
    ) : KQLObject(name, description)

    class Scalar<T : Any> (
            name : String,
            val kClass: KClass<T>,
            val scalarSupport : ScalarSupport<T>,
            description : String?
    ) : KQLObject(name, description)

    class Interface<T : Any> (
            name : String,
            val kClass: KClass<T>,
            description : String?
    ) : KQLObject(name, description)

    class Enumeration<T : Enum<T>> (
            name: String,
            val kClass: KClass<T>,
            val values: Array<T>,
            description : String?
    ) : KQLObject(name, description)
}