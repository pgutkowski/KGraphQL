package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.SchemaBuilder
import kotlin.reflect.KClass


class DefaultSchemaBuilder : SchemaBuilder {

    override fun build(): Schema {
        return DefaultSchema(queries, mutations, simpleTypes, scalars)
    }

    val simpleTypes = arrayListOf<KQLObject.Simple<*>>()

    val queries = arrayListOf<KQLObject.Query<*>>()

    val scalars = arrayListOf<KQLObject.Scalar<*>>()

    val mutations = arrayListOf<KQLObject.Mutation<*>>()

    override fun <T> mutation(name: String, function: () -> T) : SchemaBuilder{
        mutations.add(KQLObject.Mutation(name, FunctionWrapper.on(function)))
        return this
    }

    override fun <T, R> mutation(name: String, function: (R) -> T) : SchemaBuilder {
        mutations.add(KQLObject.Mutation(name, FunctionWrapper.on(function)))
        return this
    }

    override fun <T, R, E> mutation(name: String, function: (R, E) -> T) : SchemaBuilder {
        mutations.add(KQLObject.Mutation(name, FunctionWrapper.on(function)))
        return this
    }

    override fun <T, R, E, W> mutation(name: String, function: (R, E, W) -> T) : SchemaBuilder {
        mutations.add(KQLObject.Mutation(name, FunctionWrapper.on(function)))
        return this
    }

    override fun <T, R, E, W, Q> mutation(name: String, function: (R, E, W, Q) -> T) : SchemaBuilder {
        mutations.add(KQLObject.Mutation(name, FunctionWrapper.on(function)))
        return this
    }

    override fun <T> query(name: String, function: () -> T) : SchemaBuilder {
        queries.add(KQLObject.Query(name, FunctionWrapper.on(function)))
        return this
    }

    override fun <T, R> query(name: String, function: (R) -> T) : SchemaBuilder {
        queries.add(KQLObject.Query(name, FunctionWrapper.on(function)))
        return this
    }

    override fun <T, R, E> query(name: String, function: (R, E) -> T) : SchemaBuilder {
        queries.add(KQLObject.Query(name, FunctionWrapper.on(function)))
        return this
    }

    override fun <T, R, E, W> query(name: String, function: (R, E, W) -> T) : SchemaBuilder {
        queries.add(KQLObject.Query(name, FunctionWrapper.on(function)))
        return this
    }

    override fun <T, R, E, W, Q> query(name: String, function: (R, E, W, Q) -> T) : SchemaBuilder {
        queries.add(KQLObject.Query(name, FunctionWrapper.on(function)))
        return this
    }

    override fun <T: Any> scalar(kClass: KClass<T>, scalarSupport: ScalarSupport<T>): SchemaBuilder {
        val scalar = KQLObject.Scalar(kClass.typeName(), kClass, scalarSupport)
        scalars.add(scalar)
        return this
    }

    override fun <T : Any> type(kClass: KClass<T>) : SchemaBuilder {
        simpleTypes.add(KQLObject.Simple(kClass.typeName(), kClass))
        return this
    }

    fun <T : Any> KClass<T>.typeName() = this.simpleName!!.decapitalize()
}