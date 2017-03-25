package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.schema.Schema
import com.github.pgutkowski.kql.schema.SchemaBuilder
import com.github.pgutkowski.kql.support.FieldSupport
import com.github.pgutkowski.kql.support.MutationHandler
import com.github.pgutkowski.kql.support.QueryResolver
import com.github.pgutkowski.kql.support.ScalarSupport
import kotlin.reflect.KClass


open class DefaultSchemaBuilder : SchemaBuilder {

    val types = hashMapOf<String, MutableList<KQLType<*>>>()

    override fun <T: Any>addQuery(kClass: KClass<T>, queryResolvers: List<QueryResolver<T>>, fieldSupports: List<FieldSupport<T>>): SchemaBuilder {
        if(queryResolvers.isEmpty()){
            throw IllegalArgumentException("Query type $kClass must be supported by at least 1 instance of ${QueryResolver::class}")
        }
        addType(KQLType.Query(kClass.simpleName!!, kClass, queryResolvers, fieldSupports))
        return this
    }

    override fun <T: Any>addMutation(kClass: KClass<T>, mutationHandlers: List<MutationHandler<T>>): SchemaBuilder {
        addType(KQLType.Mutation(kClass.simpleName!!, kClass, mutationHandlers))
        return this
    }

    override fun <T: Any>addInput(kClass: KClass<T>): SchemaBuilder {
        addType(KQLType.Input(kClass.simpleName!!, kClass))
        return this
    }

    override fun <T: Any, S>addScalar(kClass: KClass<T>, scalarSupport: ScalarSupport<T, S>): SchemaBuilder {
        addType(KQLType.Scalar(kClass.simpleName!!, kClass, scalarSupport))
        return this
    }

    private fun <T : Any> addType(type: KQLType<T>) {
        val simpleName = type.name
        types.putIfAbsent(simpleName, arrayListOf())
        types[simpleName]?.add(type)
    }

    override fun build(): Schema {
        return DefaultSchema(types)
    }
}
