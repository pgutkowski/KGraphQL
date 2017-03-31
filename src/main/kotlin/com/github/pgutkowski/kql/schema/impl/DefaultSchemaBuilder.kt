package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.annotation.method.ResolvingFunction
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

    val queries = arrayListOf<KQLObject.QueryField<*>>()

    val scalars = arrayListOf<KQLObject.Scalar<*>>()

    val mutations = arrayListOf<KQLObject.Mutation>()

    val inputs = arrayListOf<KQLObject.Input<*>>()

    override fun <T: Any> addQueryField(kClass: KClass<T>, queryResolvers: List<QueryResolver<T>>, fieldSupports: List<FieldResolver<T>>): SchemaBuilder {
        return addQueryField(kClass.typeName(), kClass, queryResolvers, fieldSupports)
    }

    override fun <T: Any> addQueryField(name: String, kClass: KClass<T>, queryResolvers: List<QueryResolver<T>>, fieldSupports: List<FieldResolver<T>>): SchemaBuilder {
        if(queryResolvers.isEmpty()){
            throw IllegalArgumentException("Query type $kClass must be supported by at least 1 instance of ${QueryResolver::class}")
        }
        val query = KQLObject.QueryField(name, kClass, queryResolvers, fieldSupports)
        queries.add(query)
        addType(query)
        return this
    }

    override fun addMutations(mutationResolver: MutationResolver): SchemaBuilder {
        val mutationFunctions = mutationResolver.javaClass.kotlin.functions.filter { func -> func.annotations.any { it is ResolvingFunction } }
        val mutation = KQLObject.Mutation(mutationResolver.javaClass.simpleName!!.decapitalize(), mutationResolver, mutationFunctions)
        mutations.add(mutation)
        addType(mutation)
        return this
    }

    override fun <T: Any>addInput(kClass: KClass<T>): SchemaBuilder {
        val input = KQLObject.Input(kClass.typeName(), kClass)
        inputs.add(input)
        addType(input)
        return this
    }

    override fun <T: Any>addScalar(kClass: KClass<T>, scalarSupport: ScalarSupport<T>): SchemaBuilder {
        val scalar = KQLObject.Scalar(kClass.typeName(), kClass, scalarSupport)
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

    fun <T : Any> KClass<T>.typeName() = this.simpleName!!.decapitalize()
}
