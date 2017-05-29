package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.ValidationException
import com.github.pgutkowski.kgraphql.graph.Fragment
import com.github.pgutkowski.kgraphql.graph.GraphNode
import com.github.pgutkowski.kgraphql.request.Operation
import com.github.pgutkowski.kgraphql.schema.SchemaException
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

        fun of(schema: SchemaModel) : SchemaStructure {
            return SchemaStructureBuilder(
                    schema.queries, schema.mutations, schema.objects,
                    schema.scalars, schema.enums, schema.unions
            ).build()
        }
    }

    fun createExecutionPlan(request: Operation) : ExecutionPlan {
        val children = mutableListOf<ExecutionNode.Operation<*>>()
        val root = when(request.action){
            Operation.Action.QUERY -> queries
            Operation.Action.MUTATION -> mutations
            else -> {
                val keys = request.graph.nodes.map { it.key }
                if(keys.all { queries.containsKey(it) }) {
                    queries
                } else if(keys.all { mutations.containsKey(it) }){
                    mutations
                } else {
                    keys.forEach { key ->
                        if(queries.none { it.key == key } && mutations.none{ it.key == key }){
                            throw SyntaxException("$key is not supported by this schema")
                        }
                    }
                    throw SyntaxException("Cannot infer operation from fields")
                }
            }
        }

        for(requestNode in request.graph){
            val operation = root[requestNode.key]
                    ?: throw SyntaxException("${requestNode.key} is not supported by this schema")
            children.add(handleOperation(requestNode, operation))
        }

        return ExecutionPlan(children)
    }

    private fun <T>handleOperation(requestNode: GraphNode, operation: SchemaNode.Operation<T>): ExecutionNode.Operation<T>{
        return ExecutionNode.Operation(
                operationNode = operation,
                children = handleChildren(operation, requestNode),
                key = requestNode.key,
                alias = requestNode.alias,
                arguments = requestNode.arguments
        )
    }

    private fun handleBranch(requestNode: GraphNode, operation: SchemaNode.SingleBranch): ExecutionNode {
        return ExecutionNode(
                schemaNode = operation,
                children = handleChildren(operation, requestNode),
                key = requestNode.key,
                alias = requestNode.alias,
                arguments = requestNode.arguments
        )
    }

    private fun handleUnion(requestNode: GraphNode, property: SchemaNode.UnionProperty): ExecutionNode {
        //validate that only typed fragments are present
        val illegalChildren = requestNode.children?.filterNot {
            it is Fragment.Inline || (it is Fragment.External && it.typeCondition != null)
        }

        if(illegalChildren?.any() ?: false){
            throw SyntaxException(
                    "Invalid selection set with properties: $illegalChildren " +
                    "on union type property ${property.kqlProperty.name} : ${property.returnTypes.map { it.kqlType.name }}"
            )
        }

        val unionMembersChildren = property.returnTypes.associate { returnType ->
            val fragmentRequestNode = requestNode.children?.get("on ${returnType.kqlType.name}")
                    ?: requestNode.children?.filterIsInstance<Fragment.External>()?.find {returnType.kqlType.name == it.typeCondition }
                    ?: throw SyntaxException("Missing type argument for type ${returnType.kqlType.name}")

            returnType to handleReturnType(returnType, fragmentRequestNode)
        }
        return ExecutionNode.Union (
                property,
                unionMembersChildren,
                requestNode.key,
                requestNode.alias
        )
    }

    private fun handleChildren(operation: SchemaNode.SingleBranch, requestNode: GraphNode): List<ExecutionNode> {
        return handleReturnType(operation.returnType, requestNode)
    }

    fun handleReturnType(returnType: SchemaNode.ReturnType, requestNode: GraphNode) : List<ExecutionNode>{
        val children = mutableListOf<ExecutionNode>()
        if (requestNode.children != null) {
            val childrenRequestNodes = requestNode.children
            for (childRequestNode in childrenRequestNodes) {
                children.addAll(handleReturnTypeChildOrFragment(childRequestNode, returnType))
            }
        }
        return children
    }

    private fun handleReturnTypeChildOrFragment(childRequestNode: GraphNode, returnType: SchemaNode.ReturnType): List<ExecutionNode> {
        val children = mutableListOf<ExecutionNode>()
        if (childRequestNode.key.startsWith("...")) {
            if (childRequestNode is Fragment.External) {
                childRequestNode.fragmentGraph.mapTo(children) { handleReturnTypeChild(it, returnType) }
            } else {
                throw ExecutionException("Unexpected property starting with '...' ${childRequestNode.key}")
            }
        } else {
            children.add(handleReturnTypeChild(childRequestNode, returnType))
        }
        return children
    }

    private fun handleReturnTypeChild(childRequestNode: GraphNode, returnType: SchemaNode.ReturnType): ExecutionNode {
        val property = returnType.properties[childRequestNode.key]
        val unionProperty = returnType.unionProperties[childRequestNode.key]

        when {
            property == null && unionProperty == null -> {
                throw SyntaxException("property ${childRequestNode.key} on ${returnType.kqlType.name} does not exist")
            }
            property != null && unionProperty == null -> {
                val kqlType = returnType.kqlType
                val kqlProperty = property.kqlProperty
                validatePropertyArguments(kqlProperty, kqlType, childRequestNode, property.transformation)
                return handleBranch(childRequestNode, property)
            }
            property == null && unionProperty != null -> {
                return handleUnion(childRequestNode, unionProperty)
            }
            else -> throw SchemaException("Invalid schema structure - type contains doubling properties")
        }
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