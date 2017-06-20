package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.schema.model.KQLType
import kotlin.reflect.KClass
import kotlin.reflect.KType


interface TypeDefinitionProvider {

    fun typeByKClass(kClass: KClass<*>) : KQLType?

    fun typeByKType(kType: KType) : KQLType?

    fun inputTypeByKClass(kClass: KClass<*>) : KQLType?

    fun inputTypeByKType(kType: KType) : KQLType?
}