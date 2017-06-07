package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.ValidationException
import com.github.pgutkowski.kgraphql.graph.DirectiveInvocation
import com.github.pgutkowski.kgraphql.graph.Fragment
import com.github.pgutkowski.kgraphql.graph.GraphNode
import com.github.pgutkowski.kgraphql.request.Operation
import com.github.pgutkowski.kgraphql.request.Variable
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.directive.Directive
import com.github.pgutkowski.kgraphql.schema.execution.Execution
import com.github.pgutkowski.kgraphql.schema.execution.ExecutionPlan
import com.github.pgutkowski.kgraphql.schema.execution.TypeCondition
import com.github.pgutkowski.kgraphql.schema.model.*
import kotlin.reflect.KType


class SchemaStructure (
        val queries : Map<String, SchemaNode.Query<*>>,
        val mutations : Map<String, SchemaNode.Mutation<*>>,
        val nodes : Map<KType, SchemaNode.Type>,
        val directives: Map<String, Directive>
) {

    companion object {
        fun of (
                queries: List<KQLQuery<*>>,
                mutations: List<KQLMutation<*>>,
                objects: List<KQLType.Object<*>>,
                scalars: List<KQLType.Scalar<*>>,
                enums: List<KQLType.Enumeration<*>>,
                unions: List<KQLType.Union>,
                directives: List<Directive>
        ): SchemaStructure {
            return SchemaStructureBuilder(queries, mutations, objects, scalars, enums, unions, directives).build()
        }

        fun of(schema: SchemaModel) : SchemaStructure {
            with(schema) {
                return SchemaStructureBuilder(
                        queries,
                        mutations,
                        objects,
                        scalars,
                        enums,
                        unions,
                        directives
                ).build()
            }
        }
    }

    fun createExecutionPlan(request: Operation) : ExecutionPlan {
        val children = mutableListOf<Execution.Operation<*>>()
        val root = getRoot(request)

        for(requestNode in request.graph){
            val operation = root[requestNode.key]
                    ?: throw SyntaxException("${requestNode.key} is not supported by this schema")
            children.add(handleOperation(requestNode, operation, request.variables))
        }

        return ExecutionPlan(children)
    }

    private fun getRoot(request: Operation): Map<String, SchemaNode.Operation<*>> {
        return when (request.action) {
            Operation.Action.QUERY -> queries
            Operation.Action.MUTATION -> mutations
            else -> {
                val keys = request.graph.nodes.map { it.key }
                when {
                    keys.all { queries.containsKey(it) } -> queries
                    keys.all { mutations.containsKey(it) } -> mutations
                    else -> {
                        handleUnsupportedOperations(keys)
                        throw SyntaxException("Cannot infer operation from fields")
                    }
                }
            }
        }
    }

    private fun handleUnsupportedOperations(keys: List<String>) {
        keys.forEach { key ->
            if (queries.none { it.key == key } && mutations.none { it.key == key }) {
                throw SyntaxException("$key is not supported by this schema")
            }
        }
    }

    private fun <T>handleOperation(requestNode: GraphNode,
                                   operation: SchemaNode.Operation<T>,
                                   variables: List<Variable>?): Execution.Operation<T>{
        return Execution.Operation(
                operationNode = operation,
                children = handleChildren(operation, requestNode),
                key = requestNode.key,
                alias = requestNode.alias,
                arguments = requestNode.arguments,
                directives = requestNode.directives?.lookup(),
                variables = variables
        )
    }

    private fun handleBranch(requestNode: GraphNode, operation: SchemaNode.SingleBranch): Execution.Node {
        return Execution.Node(
                schemaNode = operation,
                children = handleChildren(operation, requestNode),
                key = requestNode.key,
                alias = requestNode.alias,
                arguments = requestNode.arguments,
                directives = requestNode.directives?.lookup()
        )
    }

    fun List<DirectiveInvocation>.lookup() = associate { findDirective(it) to it.arguments }

    private fun handleUnion(requestNode: GraphNode, property: SchemaNode.UnionProperty): Execution.Union {
        validateUnionRequest(requestNode, property)

        val unionMembersChildren = property.returnTypes.associate { returnType ->
            val fragmentRequestNode = requestNode.children?.get("on ${returnType.kqlType.name}")
                    ?: requestNode.children?.filterIsInstance<Fragment.External>()?.find {returnType.kqlType.name == it.typeCondition }
                    ?: throw SyntaxException("Missing type argument for type ${returnType.kqlType.name}")

            returnType to handleReturnType(returnType, fragmentRequestNode)
        }
        return Execution.Union (
                unionNode = property,
                memberChildren = unionMembersChildren,
                key = requestNode.key,
                alias = requestNode.alias,
                condition = null,
                directives = requestNode.directives?.lookup()
        )
    }

    /**
     * validate that only typed fragments are present
     */
    private fun validateUnionRequest(requestNode: GraphNode, property: SchemaNode.UnionProperty) {
        val illegalChildren = requestNode.children?.filterNot {
            it is Fragment.Inline || it is Fragment.External
        }

        if (illegalChildren?.any() ?: false) {
            throw SyntaxException(
                    "Invalid selection set with properties: $illegalChildren " +
                            "on union type property ${property.kqlProperty.name} : ${property.returnTypes.map { it.kqlType.name }}"
            )
        }
    }

    private fun handleChildren(operation: SchemaNode.SingleBranch, requestNode: GraphNode): List<Execution> {
        return handleReturnType(operation.returnType, requestNode)
    }

    fun handleReturnType(returnType: SchemaNode.ReturnType, requestNode: GraphNode) : List<Execution>{
        val children = mutableListOf<Execution>()
        if (requestNode.children != null) {
            val childrenRequestNodes = requestNode.children
            for (childRequestNode in childrenRequestNodes) {
                children.addAll(handleReturnTypeChildOrFragment(childRequestNode, returnType))
            }
        } else if(returnType.properties.isNotEmpty()){
            throw SyntaxException("Missing selection set on property ${requestNode.aliasOrKey} of type ${returnType.kqlType.name}")
        }
        return children
    }

    private fun handleReturnTypeChildOrFragment(node: GraphNode, returnType: SchemaNode.ReturnType): List<Execution> {
        val children = mutableListOf<Execution>()
        when(node){
            is Fragment -> {
                val type = if(node.typeCondition == null && node.directives?.isNotEmpty() ?: false){
                    returnType.type
                } else {
                    nodes.values.find { it.kqlType.name == node.typeCondition }
                            ?: throw SyntaxException("Unknown type ${node.typeCondition} in type condition on fragment ${node.aliasOrKey}")
                }

                val condition = TypeCondition(type)
                val elements = node.fragmentGraph.map { handleTypeChild(it, type) }
                children.add(Execution.Fragment(condition, elements, node.directives?.lookup()))
            }
            else -> {
                children.add(handleTypeChild(node, returnType))
            }
        }
        return children
    }

    private fun handleTypeChild(childRequestNode: GraphNode, returnType: SchemaNode.Type): Execution.Node {
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

    fun findDirective(invocation : DirectiveInvocation) : Directive {
        return directives[invocation.key.removePrefix("@")] ?: throw SyntaxException("Directive ${invocation.key} does not exist")
    }
}