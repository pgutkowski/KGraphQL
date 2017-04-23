package com.github.pgutkowski.kgraphql.schema

import com.github.pgutkowski.kgraphql.schema.impl.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.impl.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.impl.KQLObject
import com.github.pgutkowski.kgraphql.typeName


class SchemaBuilder {
    fun build(): Schema {
        return DefaultSchema(queries, mutations, simpleTypes, scalars, enums)
    }

    val simpleTypes = arrayListOf<KQLObject.Simple<*>>()

    val queries = arrayListOf<KQLObject.Query<*>>()

    val scalars = arrayListOf<KQLObject.Scalar<*>>()

    val mutations = arrayListOf<KQLObject.Mutation<*>>()

    val enums = arrayListOf<KQLObject.Enumeration<*>>()

    fun <T> mutation(name: String, function: () -> T): SchemaBuilder {
        mutations.add(KQLObject.Mutation(name, FunctionWrapper.on(function)))
        return this
    }

    fun <T, R> mutation(name: String, function: (R) -> T): SchemaBuilder {
        mutations.add(KQLObject.Mutation(name, FunctionWrapper.on(function)))
        return this
    }

    fun <T, R, E> mutation(name: String, function: (R, E) -> T): SchemaBuilder {
        mutations.add(KQLObject.Mutation(name, FunctionWrapper.on(function)))
        return this
    }

    fun <T, R, E, W> mutation(name: String, function: (R, E, W) -> T): SchemaBuilder {
        mutations.add(KQLObject.Mutation(name, FunctionWrapper.on(function)))
        return this
    }

    fun <T, R, E, W, Q> mutation(name: String, function: (R, E, W, Q) -> T): SchemaBuilder {
        mutations.add(KQLObject.Mutation(name, FunctionWrapper.on(function)))
        return this
    }

    fun <T> query(name: String, function: () -> T): SchemaBuilder {
        queries.add(KQLObject.Query(name, FunctionWrapper.on(function)))
        return this
    }

    fun <T, R> query(name: String, function: (R) -> T): SchemaBuilder {
        queries.add(KQLObject.Query(name, FunctionWrapper.on(function)))
        return this
    }

    fun <T, R, E> query(name: String, function: (R, E) -> T): SchemaBuilder {
        queries.add(KQLObject.Query(name, FunctionWrapper.on(function)))
        return this
    }

    fun <T, R, E, W> query(name: String, function: (R, E, W) -> T): SchemaBuilder {
        queries.add(KQLObject.Query(name, FunctionWrapper.on(function)))
        return this
    }

    fun <T, R, E, W, Q> query(name: String, function: (R, E, W, Q) -> T): SchemaBuilder {
        queries.add(KQLObject.Query(name, FunctionWrapper.on(function)))
        return this
    }

    inline fun <reified T : Any> scalar(scalarSupport: ScalarSupport<T>): SchemaBuilder {
        val scalar = KQLObject.Scalar(T::class.typeName(), T::class, scalarSupport)
        scalars.add(scalar)
        return this
    }

    inline fun <reified T : Any> type(): SchemaBuilder {
        simpleTypes.add(KQLObject.Simple(T::class.typeName(), T::class))
        return this
    }

    inline fun <reified T : Enum<T>> enum(): SchemaBuilder {
        val enumValues = enumValues<T>()
        if(enumValues.isEmpty()){
            throw SchemaException("Enum of type ${T::class} must have at least one value")
        } else {
            enums.add(KQLObject.Enumeration(T::class.typeName(), T::class, enumValues))
            return this
        }
    }
}
