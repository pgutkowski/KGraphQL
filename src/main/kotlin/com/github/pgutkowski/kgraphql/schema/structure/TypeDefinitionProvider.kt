package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.schema.model.KQLType
import kotlin.reflect.KClass
import kotlin.reflect.KType


interface TypeDefinitionProvider {

    fun <T : Any> typeByKClass(kClass: KClass<T>) : KQLType?

    fun typeByKType(kType: KType) : KQLType?

    fun <T : Any> inputTypeByKClass(kClass: KClass<T>) : KQLType?

    fun inputTypeByKType(kType: KType) : KQLType?
}