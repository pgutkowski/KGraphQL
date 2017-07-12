package com.github.pgutkowski.kgraphql

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

fun <T : Any> KClass<T>.defaultKQLTypeName() = this.simpleName!!

fun KType.defaultKQLTypeName() = this.jvmErasure.defaultKQLTypeName()

fun String.dropQuotes() : String = if(isLiteral()) drop(1).dropLast(1) else this

fun String.isLiteral() : Boolean = startsWith('\"') && endsWith('\"')

fun KParameter.isNullable() = type.isMarkedNullable

fun KParameter.isNotNullable() = !type.isMarkedNullable

fun KClass<*>.isCollection() = isSubclassOf(Collection::class)

fun KType.isCollection() = jvmErasure.isCollection()

fun KType.getCollectionElementType(): KType? {
    if(!jvmErasure.isCollection()) throw IllegalArgumentException("KType $this is not collection type")
    return arguments.firstOrNull()?.type ?: throw NoSuchElementException("KType $this has no type arguments")
}

fun not(boolean: Boolean) = !boolean