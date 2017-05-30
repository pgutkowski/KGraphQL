package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.schema.model.*

/**
 * [SchemaNode] is component of [SchemaStructure] and wrapper for corresponding [KQLObject].
 * As opposed to [KQLObject]'s, [SchemaNode]s are linked to each other.
 */
sealed class SchemaNode {

    open class Type(
            val kqlType: com.github.pgutkowski.kgraphql.schema.model.KQLType,
            val properties : Map<String, com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.Property> = emptyMap(),
            val unionProperties: Map<String, com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.UnionProperty> = emptyMap()
    ) : com.github.pgutkowski.kgraphql.schema.structure.SchemaNode()

    class ReturnType(
            val type: com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.Type,
            val isCollection : Boolean = false,
            val isNullable: Boolean = false,
            val areEntriesNullable : Boolean = false
    ) : com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.Type(type.kqlType, type.properties, type.unionProperties)

    interface Branch

    abstract class SingleBranch(val returnType: com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.ReturnType) : com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.Branch, com.github.pgutkowski.kgraphql.schema.structure.SchemaNode()

    class UnionProperty (
            val kqlProperty: com.github.pgutkowski.kgraphql.schema.model.KQLProperty.Union,
            val returnTypes: List<com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.ReturnType>
    ) : com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.Branch, com.github.pgutkowski.kgraphql.schema.structure.SchemaNode()

    class Property(
            val kqlProperty: com.github.pgutkowski.kgraphql.schema.model.KQLProperty,
            returnType : com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.ReturnType,
            val transformation: com.github.pgutkowski.kgraphql.schema.model.Transformation<*, *>? = null
    ) : com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.SingleBranch(returnType)

    abstract class Operation<T>(
            val kqlOperation: com.github.pgutkowski.kgraphql.schema.model.KQLOperation<T>,
            returnType: com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.ReturnType
    ) : com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.SingleBranch(returnType)

    class Query<T>(
            val kqlQuery: com.github.pgutkowski.kgraphql.schema.model.KQLQuery<T>,
            returnType: com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.ReturnType
    ) : com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.Operation<T>(kqlQuery, returnType)

    class Mutation<T>(
            val kqlMutation: com.github.pgutkowski.kgraphql.schema.model.KQLMutation<T>,
            returnType: com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.ReturnType
    ) : com.github.pgutkowski.kgraphql.schema.structure.SchemaNode.Operation<T>(kqlMutation, returnType)
}