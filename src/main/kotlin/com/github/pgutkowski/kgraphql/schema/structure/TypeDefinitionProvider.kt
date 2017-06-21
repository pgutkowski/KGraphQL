package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.isCollection
import com.github.pgutkowski.kgraphql.request.TypeReference
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure


interface TypeDefinitionProvider {

    fun typeByKClass(kClass: KClass<*>) : KQLType?

    fun typeByKType(kType: KType) : KQLType?

    fun typeByName(name: String) : KQLType?

    fun inputTypeByKClass(kClass: KClass<*>) : KQLType?

    fun inputTypeByKType(kType: KType) : KQLType?

    fun inputTypeByName(name: String) : KQLType?

    fun typeReference(kType: KType) : TypeReference {
        if(kType.jvmErasure.isCollection()){
            val elementType = kType.arguments.first().type
                   ?: throw IllegalArgumentException("Cannot transform kotlin collection type $kType to KGraphQL TypeReference")
            val kqlType = typeByKType(elementType) ?: inputTypeByKType(elementType)
                    ?: throw IllegalArgumentException("$kType has not been registered in this schema")
            return TypeReference(kqlType.name, kType.isMarkedNullable, true, elementType.isMarkedNullable)
        } else {
            val kqlType = typeByKType(kType) ?: inputTypeByKType(kType)
                    ?: throw IllegalArgumentException("$kType has not been registered in this schema")
            return TypeReference(kqlType.name, kType.isMarkedNullable)
        }
    }
}