package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.annotation.*
import com.github.pgutkowski.kql.annotation.type.Input
import com.github.pgutkowski.kql.annotation.type.Query
import com.github.pgutkowski.kql.annotation.type.Scalar
import com.github.pgutkowski.kql.annotation.type.Type
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation


class ClassScannerSchemaBuilder : DefaultSchemaBuilder(){

    fun <T : Any> KClass<T>.isInterface() = java.isInterface

    fun <T : Any> KClass<T>.isQuery() = findAnnotation<Query>() != null

    fun <T : Any> KClass<T>.isInput() = findAnnotation<Input>() != null

    fun <T : Any> KClass<T>.isScalar() = findAnnotation<Scalar>() != null

    fun <T : Any> KClass<T>.isSimple(): Boolean {
        return findAnnotation<Type>() != null || !(isQuery() || isInput() || isScalar() )
    }
}