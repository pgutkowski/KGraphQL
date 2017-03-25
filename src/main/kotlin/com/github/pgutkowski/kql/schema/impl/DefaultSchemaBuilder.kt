package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.annotation.method.Mutation
import com.github.pgutkowski.kql.resolve.FieldResolver
import com.github.pgutkowski.kql.resolve.MutationResolver
import com.github.pgutkowski.kql.resolve.QueryResolver
import com.github.pgutkowski.kql.scalar.ScalarSupport
import com.github.pgutkowski.kql.schema.Schema
import com.github.pgutkowski.kql.schema.SchemaBuilder
import kotlin.reflect.KClass
import kotlin.reflect.full.functions


open class DefaultSchemaBuilder : SchemaBuilder {

    val types = hashMapOf<String, MutableList<KQLObject>>()

    val queries = arrayListOf<KQLObject.Query<*>>()

    val scalars = arrayListOf<KQLObject.Scalar<*,*>>()

    val mutations = arrayListOf<KQLObject.Mutation>()

    val inputs = arrayListOf<KQLObject.Input<*>>()

    override fun <T: Any>addQuery(kClass: KClass<T>, queryResolvers: List<QueryResolver<T>>, fieldSupports: List<FieldResolver<T>>): SchemaBuilder {
        if(queryResolvers.isEmpty()){
            throw IllegalArgumentException("Query type $kClass must be supported by at least 1 instance of ${QueryResolver::class}")
        }
        val query = KQLObject.Query(kClass.simpleName!!, kClass, queryResolvers, fieldSupports)
        queries.add(query)
        addType(query)
        return this
    }

    override fun addMutations(mutationResolver: MutationResolver): SchemaBuilder {
        val mutationFunctions = mutationResolver.javaClass.kotlin.functions.filter { func -> func.annotations.any { it is Mutation } }
        val mutation = KQLObject.Mutation(mutationResolver.javaClass.simpleName!!, mutationResolver, mutationFunctions)
        mutations.add(mutation)
        addType(mutation)
        return this
    }

    override fun <T: Any>addInput(kClass: KClass<T>): SchemaBuilder {
        val input = KQLObject.Input(kClass.simpleName!!, kClass)
        inputs.add(input)
        addType(input)
        return this
    }

    override fun <T: Any, S>addScalar(kClass: KClass<T>, scalarSupport: ScalarSupport<T, S>): SchemaBuilder {
        val scalar = KQLObject.Scalar(kClass.simpleName!!, kClass, scalarSupport)
        scalars.add(scalar)
        addType(scalar)
        return this
    }

    private fun addType(type: KQLObject) {
        val simpleName = type.name
        types.putIfAbsent(simpleName, arrayListOf())
        types[simpleName]?.add(type)
    }

    override fun build(): Schema {
        return DefaultSchema(types, queries, mutations, inputs, scalars)
    }
}
