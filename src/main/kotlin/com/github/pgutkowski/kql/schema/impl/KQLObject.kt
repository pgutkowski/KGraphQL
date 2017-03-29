package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.resolve.FieldResolver
import com.github.pgutkowski.kql.resolve.MutationResolver
import com.github.pgutkowski.kql.resolve.QueryResolver
import com.github.pgutkowski.kql.scalar.ScalarSupport
import kotlin.reflect.KClass
import kotlin.reflect.KFunction


sealed class KQLObject(val name : String) {

    class Simple<T : Any>(name : String, val kClass: KClass<T>) : KQLObject(name)

    class Mutation(name : String, val resolver: MutationResolver, val functions: List<KFunction<*>>) : KQLObject(name)

    class QueryField<T : Any>(
            name: String,
            val kClass: KClass<T>,
            val resolvers: List<QueryResolver<T>>,
            val fieldsResolvers: List<FieldResolver<T>>
    ) : KQLObject(name)

    class Scalar<T : Any>(
            name : String,
            val kClass: KClass<T>,
            val scalarSupport : ScalarSupport<T>
    ) : KQLObject(name)

    class Input<T : Any>(name : String, val kClass: KClass<T>) : KQLObject(name)

    class Interface<T : Any>(name : String, val kClass: KClass<T>) : KQLObject(name)
}