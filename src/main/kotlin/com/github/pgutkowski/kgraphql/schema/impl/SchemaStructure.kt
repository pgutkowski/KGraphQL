package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.ValidationException
import com.github.pgutkowski.kgraphql.graph.GraphNode
import com.github.pgutkowski.kgraphql.request.Request
import com.github.pgutkowski.kgraphql.schema.model.KQLMutation
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLQuery
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import kotlin.reflect.KType


class SchemaStructure(val queries : Map<String, SchemaNode.Query<*>>,
                      val mutations : Map<String, SchemaNode.Mutation<*>>,
                      val nodes : Map<KType, SchemaNode.Type>) {

    companion object {
        fun of(
                queries: List<KQLQuery<*>>,
                mutations: List<KQLMutation<*>>,
                objects: List<KQLType.Object<*>>,
                scalars: List<KQLType.Scalar<*>>,
                enums: List<KQLType.Enumeration<*>>,
                unions: List<KQLType.Union>
        ): SchemaStructure {
            return SchemaStructureBuilder(queries, mutations, objects, scalars, enums, unions).build()
        }

        fun of(schema: DefaultSchema) : SchemaStructure {
            return SchemaStructureBuilder(
                    schema.queries, schema.mutations, schema.objects,
                    schema.scalars, schema.enums, schema.unions
            ).build()
        }
    }

    fun createExecutionPlan(request: Request) : ExecutionPlan {
        val children = mutableListOf<ExecutionNode.Operation<*>>()
        val root = if(request.action == Request.Action.QUERY) queries else mutations

        for(requestNode in request.graph){
            val operation = root[requestNode.key]
                    ?: throw SyntaxException("${requestNode.key} is not supported by this schema")
            children.add(handleOperation(requestNode, operation))
        }

        return ExecutionPlan(children)
    }

    private fun <T>handleOperation(requestNode: GraphNode, operation: SchemaNode.Operation<T>): ExecutionNode.Operation<T>{
        return ExecutionNode.Operation(
                operation,
                handleChildren(operation, requestNode),
                requestNode.key,
                requestNode.alias,
                requestNode.arguments
        )
    }

    private fun handleBranch(requestNode: GraphNode, operation: SchemaNode.Branch): ExecutionNode {
        return ExecutionNode(
                operation,
                handleChildren(operation, requestNode),
                requestNode.key,
                requestNode.alias,
                requestNode.arguments
        )
    }

    private fun handleChildren(operation: SchemaNode.Branch, requestNode: GraphNode): MutableList<ExecutionNode> {
        val children = mutableListOf<ExecutionNode>()
        if (requestNode.children != null) {
            val childrenRequestNodes = requestNode.children
            for (childRequestNode in childrenRequestNodes) {
                val property = operation.returnType.properties[childRequestNode.key] ?: operation.returnType.unionProperties[childRequestNode.key]
                        ?: throw SyntaxException("property ${childRequestNode.key} on ${operation.returnType.kqlType.name} does not exist")

                when(property){
                    is SchemaNode.Property -> {
                        children.add(handleBranch(childRequestNode, property))
                        val kqlType = operation.returnType.kqlType
                        val kqlProperty = property.kqlProperty
                        validatePropertyArguments(kqlProperty, kqlType, childRequestNode, property.transformation)
                    }
                    is SchemaNode.UnionProperty -> {

                    }
                    else ->
                }

            }
        }
        return children
    }

    /**
     * needs to be simplified
     */
    private fun validatePropertyArguments(kqlProperty: KQLProperty, kqlType: KQLType, requestNode: GraphNode, transformation: Transformation<*, *>?) {

        fun illegalArguments(): List<ValidationException> {
            return listOf(ValidationException(
                    "Property ${kqlProperty.name} on type ${kqlType.name} has no arguments, found: ${requestNode.arguments?.map { it.key }}")
            )
        }

        val argumentValidationExceptions = when {
            //extension property function
            kqlProperty is KQLProperty.Function<*> -> {
                kqlProperty.validateArguments(requestNode.arguments)
            }
            //property with transformation
            kqlProperty is KQLProperty.Kotlin<*, *> && kqlType is KQLType.Object<*> -> {
                transformation
                        ?.validateArguments(requestNode.arguments)
                        ?: if(requestNode.arguments == null) emptyList() else illegalArguments()
            }
            requestNode.arguments == null -> emptyList()
            else -> illegalArguments()
        }

        if (argumentValidationExceptions.isNotEmpty()) {
            throw ValidationException(argumentValidationExceptions.fold("", { sum, exc -> sum + "${exc.message}; " }))
        }
    }
}