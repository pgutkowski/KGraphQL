package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.schema.model.KQLMutation
import com.github.pgutkowski.kgraphql.schema.model.KQLObject
import com.github.pgutkowski.kgraphql.schema.model.KQLOperation
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLQuery
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.model.Transformation

/**
 * [SchemaNode] is component of [SchemaStructure] and wrapper for corresponding [KQLObject].
 * As opposed to [KQLObject]'s, [SchemaNode]s are linked to each other.
 */
sealed class SchemaNode {

    open class Type (
            val kqlType: KQLType,
            val properties : Map<String, SchemaNode.Property> = emptyMap(),
            val unionProperties: Map<String, SchemaNode.UnionProperty> = emptyMap()
    ) : SchemaNode()

    class ReturnType(
            val type: SchemaNode.Type,
            val isCollection : Boolean = false,
            val isNullable: Boolean = false,
            val areEntriesNullable : Boolean = false
    ) : SchemaNode.Type(type.kqlType, type.properties, type.unionProperties)

    interface Branch

    abstract class SingleBranch(val returnType: SchemaNode.ReturnType) : SchemaNode.Branch, SchemaNode()

    class UnionProperty (
            val kqlProperty: KQLProperty.Union,
            val returnTypes: List<SchemaNode.ReturnType>
    ) : SchemaNode.Branch, SchemaNode()

    class Property(
            val kqlProperty: KQLProperty,
            returnType : SchemaNode.ReturnType,
            val transformation: Transformation<*, *>? = null
    ) : SchemaNode.SingleBranch(returnType)

    abstract class Operation<T>(
            val kqlOperation: KQLOperation<T>,
            returnType: SchemaNode.ReturnType
    ) : SchemaNode.SingleBranch(returnType)

    class Query<T>(
            val kqlQuery: KQLQuery<T>,
            returnType: SchemaNode.ReturnType
    ) : SchemaNode.Operation<T>(kqlQuery, returnType)

    class Mutation<T>(
            val kqlMutation: KQLMutation<T>,
            returnType: SchemaNode.ReturnType
    ) : SchemaNode.Operation<T>(kqlMutation, returnType)
}