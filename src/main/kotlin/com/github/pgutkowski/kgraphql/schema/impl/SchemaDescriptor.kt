package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.graph.DescriptorNode
import com.github.pgutkowski.kgraphql.graph.Graph
import com.github.pgutkowski.kgraphql.schema.SchemaException
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure


class SchemaDescriptor private constructor(val queries: Graph, val mutations: Graph, internal val typeMap: Map<KClass<*>, Graph>, val enums: List<KQLObject.Enumeration<*>>) {

    companion object {
        fun forSchema (schema: DefaultSchema): SchemaDescriptor {

            val typeChildren: MutableMap<KClass<*>, Graph> = mutableMapOf()

            fun handleType(key : String, kType: KType, createArguments: ()->Map<String, KType>, isCollectionElement: Boolean = false) : DescriptorNode {

                fun <T : Any>isScalar(kClass: KClass<T>): Boolean {
                    return schema.scalars.any { scalar ->
                        scalar.kClass.isSuperclassOf(kClass)
                    }
                }

                fun <T : Any>handleComplexType(key : String, kClass: KClass<T>, createArguments: ()->Map<String, KType>, isCollectionElement: Boolean): DescriptorNode {
                    val cachedChildren = typeChildren[kClass]
                    val children : Graph
                    if(cachedChildren == null){
                        children = Graph()
                        kClass.memberProperties.forEach { property ->
                            children.add(handleType(property.name, property.returnType, { emptyMap()}))
                        }
                        typeChildren.put(kClass, children)
                    } else {
                        children = cachedChildren
                    }
                    return DescriptorNode(key, kClass, children, createArguments(), isCollectionElement)
                }

                val kClass = kType.jvmErasure
                return when {
                    /*BUILT IN TYPE:*/
                    kClass in DefaultSchema.BUILT_IN_TYPES -> {
                        DescriptorNode(key, kType, createArguments())
                    }

                    /*Collections*/
                    kClass.isSubclassOf(Collection::class) -> {
                        val collectionType = kType.arguments.first().type
                                ?: throw IllegalArgumentException("Failed to create descriptor for type: $kType")

                        handleType(key, collectionType, createArguments, true)
                    }

                    /*Scalar*/
                    isScalar(kClass) -> DescriptorNode(key, kType, createArguments())

                    /*Enums*/
                    kClass.isSubclassOf(Enum::class) -> {
                        DescriptorNode(key, kType, createArguments())
                    }

                    /* every other type is treated as graph and split*/
                    else -> {
                        handleComplexType(key, kClass, createArguments, isCollectionElement)
                    }
                }
            }

            fun <T>handleFunctionWrapper(name: String, function: FunctionWrapper<T>): DescriptorNode {

                fun createArguments(): MutableMap<String, KType> {
                    val arguments : MutableMap<String, KType> = mutableMapOf()
                    function.kFunction.valueParameters.forEach { parameter ->
                        if(parameter.name == null) throw SchemaException("Cannot create descriptor for nameless field on function: ${function.kFunction}")
                        arguments[parameter.name!!] = parameter.type
                    }
                    return arguments
                }

                if(function.kFunction.instanceParameter != null){
                    throw SchemaException("Schema cannot use class methods, please use local or lambda functions to wrap them")
                }

                return handleType(name, function.kFunction.returnType, ::createArguments)
            }

            val queries = Graph()
            val mutations = Graph()
            schema.queries.forEach { query -> queries.add(handleFunctionWrapper(query.name, query)) }
            schema.mutations.forEach { mutation -> mutations.add(handleFunctionWrapper(mutation.name, mutation)) }
            return SchemaDescriptor(queries, mutations, typeChildren, schema.enums)
        }
    }

    fun <T : Any>get(key: KClass<T>) = this.typeMap[key]

    fun validateRequestGraph(graph: Graph){
        for(node in graph){
            graph[node.aliasOrKey] ?: throw IllegalArgumentException("${node.key} does not exist in this schema")
            //should validate children and arguments
        }
    }
}