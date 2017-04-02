package com.github.pgutkowski.kql.schema

import com.github.pgutkowski.kql.resolve.FieldResolver
import com.github.pgutkowski.kql.resolve.MutationResolver
import com.github.pgutkowski.kql.resolve.QueryResolver
import com.github.pgutkowski.kql.schema.ScalarSupport
import kotlin.reflect.KClass


interface SchemaBuilder {

    fun build() : Schema

    fun <T: Any> addQueryField(kClass: KClass<T>, queryResolvers: List<QueryResolver<T>>, fieldSupports: List<FieldResolver<T>> = emptyList()): SchemaBuilder

    fun <T: Any> addQueryField(name: String, kClass: KClass<T>, queryResolvers: List<QueryResolver<T>>, fieldSupports: List<FieldResolver<T>> = emptyList()): SchemaBuilder

    fun addMutations(mutationResolver: MutationResolver): SchemaBuilder

    fun <T: Any>addInput(kClass: KClass<T>): SchemaBuilder

    fun <T: Any>addScalar(kClass: KClass<T>, scalarSupport: ScalarSupport<T>): SchemaBuilder
}