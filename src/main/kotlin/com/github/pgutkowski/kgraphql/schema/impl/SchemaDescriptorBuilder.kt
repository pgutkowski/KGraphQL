package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.graph.DescriptorNode
import com.github.pgutkowski.kgraphql.graph.Graph
import com.github.pgutkowski.kgraphql.graph.GraphBuilder
import com.github.pgutkowski.kgraphql.schema.SchemaException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

class SchemaDescriptorBuilder(val schema: DefaultSchema) {

    val typeChildren: MutableMap<KClass<*>, Graph> = mutableMapOf()

    fun build() : SchemaDescriptor {
        val queries = GraphBuilder()
        val mutations = GraphBuilder()
        schema.queries.forEach { query -> queries.add(handleFunctionWrapper(query.name, query)) }
        schema.mutations.forEach { mutation -> mutations.add(handleFunctionWrapper(mutation.name, mutation)) }
        return SchemaDescriptor(schema, queries.build(), mutations.build(), typeChildren)
    }

    //TODO: refactor to reduce complexity
    fun handleType(key : String, kType: KType, createArguments: ()->Map<String, KType>, isCollectionElement: Boolean = false) : DescriptorNode {

        fun <T : Any> notIgnored(property: KProperty1<T, *>, objectDefinition: KQLObject.Object<*>?): Boolean {
            return !(objectDefinition?.ignoredProperties?.contains(property) ?: false)
        }

        fun <T : Any>handleComplexType(key : String, kClass: KClass<T>, createArguments: ()->Map<String, KType>, isCollectionElement: Boolean): DescriptorNode {
            val cachedChildren = typeChildren[kClass]
            val children : Graph
            if(cachedChildren == null){
                val objectDefinition = schema.types.find { it.kClass == kClass }
                val childrenBuilder = GraphBuilder()
                kClass.memberProperties.forEach { property ->
                    if(notIgnored(property, objectDefinition)){
                        childrenBuilder.add(handleType(property.name, property.returnType, { emptyMap() }))
                    }
                }
                children = childrenBuilder.build()
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
            schema.isScalar(kClass) -> {
                DescriptorNode(key, kType, createArguments())
            }

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

    fun <T : Any>DefaultSchema.isScalar(kClass: KClass<T>): Boolean {
        return scalars.any { scalar ->
            scalar.kClass.isSuperclassOf(kClass)
        }
    }
}
