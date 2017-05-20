package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.schema.impl.FunctionWrapper
import kotlin.reflect.KType


interface KQLOperation<T> : FunctionWrapper<T> {

    val name : String

    val argumentsDescriptor : Map<String, KType>
}