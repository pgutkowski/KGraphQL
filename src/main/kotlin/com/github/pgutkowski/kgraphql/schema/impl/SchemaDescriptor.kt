package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.request.Arguments
import com.github.pgutkowski.kgraphql.request.Graph
import com.github.pgutkowski.kgraphql.request.GraphNode
import com.github.pgutkowski.kgraphql.schema.SchemaException
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure


class SchemaDescriptor private constructor(private val queries: Map<String, DescriptorNode>, private val mutations: Map<String, DescriptorNode>) {

    companion object {
        fun forSchema (schema: DefaultSchema): SchemaDescriptor {

            val typeChildrenCache: MutableMap<KClass<*>, Map<String, DescriptorNode>> = mutableMapOf()

            fun handleType(kType: KType, createArguments: ()->Map<String, DescriptorNode>, isCollectionElement: Boolean = false) : DescriptorNode {

                fun <T : Any>isScalar(kClass: KClass<T>): Boolean {
                    return schema.scalars.any { scalar ->
                        scalar.kClass.isSuperclassOf(kClass)
                    }
                }

                val kClass = kType.jvmErasure
                return when {
                    kClass in DefaultSchema.BUILT_IN_TYPES -> {
                        DescriptorNode.Leaf(createArguments(), kType)
                    }
                    kClass.isSubclassOf(Collection::class) -> {
                        val collectionType = kType.arguments.first().type
                                ?: throw IllegalArgumentException("Failed to create descriptor for type: $kType")

                        handleType(collectionType, createArguments, true)
                    }
                    isScalar(kClass) -> DescriptorNode.Leaf(createArguments(), kType)
                    else -> {
                        val cachedChildren = typeChildrenCache[kClass]
                        val children : MutableMap<String, DescriptorNode> = mutableMapOf()
                        if(cachedChildren == null){
                            kClass.memberProperties.forEach { property ->
                                children[property.name] = handleType(property.returnType, { emptyMap()})
                            }
                            typeChildrenCache.put(kClass, children)
                        } else {
                            children.putAll(cachedChildren)
                        }
                        DescriptorNode.Branch(createArguments(), children, isCollectionElement)
                    }
                }
            }

            fun <T>handleFunctionWrapper(function: FunctionWrapper<T>): DescriptorNode {

                fun createArguments(): MutableMap<String, DescriptorNode> {
                    val arguments : MutableMap<String, DescriptorNode> = mutableMapOf()
                    function.kFunction.valueParameters.forEach { parameter ->
                        if(parameter.name == null) throw SchemaException("Cannot create descriptor for nameless field on function: ${function.kFunction}")
                        arguments[parameter.name!!] = handleType(parameter.type, createArguments = { emptyMap() })
                    }
                    return arguments
                }

                if(function.kFunction.instanceParameter != null){
                    throw SchemaException("Schema cannot use class methods, please use local or lambda functions to wrap them")
                }

                return handleType(function.kFunction.returnType, ::createArguments)
            }

            val queries : MutableMap<String, DescriptorNode> = mutableMapOf()
            for(query in schema.queries){
                queries.put(query.name, handleFunctionWrapper(query))
            }

            val mutations : MutableMap<String, DescriptorNode> = mutableMapOf()
            for(mutation in schema.mutations){
                mutations.put(mutation.name, handleFunctionWrapper(mutation))
            }

            return SchemaDescriptor(queries, mutations)
        }
    }

    private sealed class DescriptorNode(val arguments: Map<String, DescriptorNode>?, val isCollection : Boolean) {
        class Branch(
                arguments: Map<String, DescriptorNode>?,
                val children: Map<String, DescriptorNode>,
                isCollection : Boolean = false
        ) : DescriptorNode(arguments, isCollection)

        class Leaf(
                arguments: Map<String, DescriptorNode>?,
                val type: KType, isCollection :
                Boolean = false
        ) : DescriptorNode(arguments, isCollection)
    }

    fun validateQueryGraph(graph: Graph){
        validateGraph(graph, queries)
    }

    fun validateMutationGraph(graph: Graph){
        validateGraph(graph, mutations)
    }

    private fun validateGraph(graph : Graph, descriptors: Map<String, DescriptorNode>){
        for(node in graph){
            val query = descriptors[node.key] ?: throw IllegalArgumentException("${node.key} does not exist in this schema")
            validateArguments(query, node)
            validateChildren(query, node)
        }
    }

    private fun validateChildren(descriptor: DescriptorNode, node: GraphNode) {
        //TODO
    }

    private fun validateArguments(query: DescriptorNode, node: GraphNode) {
        //TODO
    }
}