package com.github.pgutkowski.kgraphql.schema.model

import kotlin.reflect.KType


interface KQLOperation<T> : FunctionWrapper<T>, Depreciable, DescribedKQLObject {

    val name : String

    val argumentsDescriptor : Map<String, KType>
}