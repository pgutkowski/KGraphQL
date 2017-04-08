package com.github.pgutkowski.kql.schema

import kotlin.reflect.KClass


interface SchemaBuilder {

    fun build() : Schema

    fun <T> mutation(name: String, function: () -> T): SchemaBuilder
    fun <T, R> mutation(name: String, function: (R) -> T): SchemaBuilder
    fun <T, R, E> mutation(name: String, function: (R, E) -> T): SchemaBuilder
    fun <T, R, E, W> mutation(name: String, function: (R, E, W) -> T): SchemaBuilder
    fun <T, R, E, W, Q> mutation(name: String, function: (R, E, W, Q) -> T): SchemaBuilder

    fun <T> query(name: String, function: () -> T): SchemaBuilder
    fun <T, R> query(name: String, function: (R) -> T): SchemaBuilder
    fun <T, R, E> query(name: String, function: (R, E) -> T): SchemaBuilder
    fun <T, R, E, W> query(name: String, function: (R, E, W) -> T): SchemaBuilder
    fun <T, R, E, W, Q> query(name: String, function: (R, E, W, Q) -> T): SchemaBuilder

    fun <T: Any> input(kClass: KClass<T>): SchemaBuilder
    fun <T: Any> scalar(kClass: KClass<T>, scalarSupport: ScalarSupport<T>): SchemaBuilder
    fun <T : Any> type(kClass: KClass<T>) : SchemaBuilder
}