package com.github.pgutkowski.kgraphql.graph

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType


class DescriptorNode(
        key : String,
        val type: KType,
        children: Graph?,
        val argumentsDescriptor: Map<String, KType>?,
        val isCollection : Boolean = false
) : GraphNode (
        key = key,
        children = children,
        alias = null,
        arguments = null
) {
    constructor(key : String, type: KType, argumentsDescriptor: Map<String, KType>?, isCollection: Boolean = false)
            : this(key, type, null, argumentsDescriptor, isCollection)

    constructor(key : String, kClass: KClass<*>, children: Graph?, argumentsDescriptor: Map<String, KType>?, isCollection: Boolean)
            : this(key, kClass.starProjectedType, children, argumentsDescriptor, isCollection)

    companion object {
        inline fun <reified T>leaf(key : String) : DescriptorNode {
            return DescriptorNode(key, T::class.starProjectedType, emptyMap())
        }

        inline fun <reified T>branch(key : String, vararg nodes : DescriptorNode): DescriptorNode {
            return DescriptorNode(key, T::class.starProjectedType, Graph(*nodes), emptyMap())
        }
    }
}