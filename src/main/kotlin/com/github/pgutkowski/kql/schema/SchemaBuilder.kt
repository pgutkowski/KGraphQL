package com.github.pgutkowski.kql.schema

import com.github.pgutkowski.kql.support.FieldSupport
import com.github.pgutkowski.kql.support.MutationHandler
import com.github.pgutkowski.kql.support.QueryResolver
import com.github.pgutkowski.kql.support.ScalarSupport
import kotlin.reflect.KClass


interface SchemaBuilder {

    fun build() : Schema

    fun <T: Any>addQuery(kClass: KClass<T>, queryResolvers: List<QueryResolver<T>>, fieldSupports: List<FieldSupport<T>> = emptyList()): SchemaBuilder

    fun <T: Any>addInput(kClass: KClass<T>): SchemaBuilder

    fun <T: Any>addMutation(kClass: KClass<T>, mutationHandlers: List<MutationHandler<T>>): SchemaBuilder

    fun <T: Any, S>addScalar(kClass: KClass<T>, scalarSupport: ScalarSupport<T, S>): SchemaBuilder
}