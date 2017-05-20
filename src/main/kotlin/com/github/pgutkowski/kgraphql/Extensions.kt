package com.github.pgutkowski.kgraphql

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

fun <T : Any> KClass<T>.typeName() = this.simpleName!!

fun KType.typeName() = this.jvmErasure.typeName()

fun String.dropQuotes() : String = if(startsWith('\"') && endsWith('\"')) drop(1).dropLast(1) else this

fun KParameter.isNullable() = type.isMarkedNullable

fun KParameter.isNotNullable() = !type.isMarkedNullable