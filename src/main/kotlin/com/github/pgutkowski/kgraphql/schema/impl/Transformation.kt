package com.github.pgutkowski.kgraphql.schema.impl

import kotlin.reflect.KProperty1


data class Transformation<T : Any, R>(val kProperty: KProperty1<T, R>, val transformation : FunctionWrapper<*>)