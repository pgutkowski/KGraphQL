package com.github.pgutkowski.kql.schema

import com.github.pgutkowski.kql.support.ClassSupport
import kotlin.reflect.KClass


interface SchemaBuilder {

    fun addClass(clazz : KClass<*>) : SchemaBuilder

    fun build() : Schema

    fun <T : Any> addSupportedClass(kClass: KClass<T>, vararg classSupport: ClassSupport<T>): SchemaBuilder
}