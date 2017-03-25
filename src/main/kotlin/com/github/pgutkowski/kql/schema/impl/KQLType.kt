package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.support.FieldSupport
import com.github.pgutkowski.kql.support.MutationHandler
import com.github.pgutkowski.kql.support.QueryResolver
import com.github.pgutkowski.kql.support.ScalarSupport
import kotlin.reflect.KClass


sealed class KQLType<T : Any>(val name : String, val kClass: KClass<T>) {

    class Simple<T : Any>(name : String, kClass: KClass<T>) : KQLType<T>(name, kClass)

    class Mutation<T : Any>(
            name : String,
            kClass: KClass<T>,
            val handlers: List<MutationHandler<T>>
    ) : KQLType<T>(name, kClass)

    class Query<T : Any>(
            name: String,
            kClass: KClass<T>,
            val resolvers: List<QueryResolver<T>>,
            val fieldSupports: List<FieldSupport<T>>
    ) : KQLType<T>(name, kClass)

    class Scalar<T : Any, O>(
            name : String,
            kClass: KClass<T>,
            val scalarSupport : ScalarSupport<T, O>
    ) : KQLType<T>(name, kClass)

    class Input<T : Any>(name : String, kClass: KClass<T>) : KQLType<T>(name, kClass)

    class Interface<T : Any>(name : String, kClass: KClass<T>) : KQLType<T>(name, kClass)
}