package com.github.pgutkowski.kgraphql.graph

import com.github.pgutkowski.kgraphql.schema.model.KQLObject
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType


class DescriptorNode(
        val kqlObject: KQLObject,
        val key : String,
        val type: KType,
        val children: Collection<DescriptorNode>,
        val isCollection : Boolean = false
) {
    constructor(kqlObject: KQLObject, key : String, type: KType, isCollection: Boolean = false)
            : this(kqlObject, key, type, emptyList(), isCollection)

    constructor(kqlObject: KQLObject, key : String, kClass: KClass<*>, children: Collection<DescriptorNode>, isCollection: Boolean)
            : this(kqlObject, key, kClass.starProjectedType, children, isCollection)

    companion object {
        inline fun <reified T>leaf(kqlObject: KQLObject, key : String) : DescriptorNode {
            return DescriptorNode(kqlObject, key, T::class.starProjectedType)
        }

        inline fun <reified T>branch(kqlObject: KQLObject, key : String, vararg nodes : DescriptorNode): DescriptorNode {
            return DescriptorNode(kqlObject, key, T::class.starProjectedType, listOf(*nodes))
        }
    }
}