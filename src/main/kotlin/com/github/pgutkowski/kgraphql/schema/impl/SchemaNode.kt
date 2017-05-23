package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.schema.model.*


sealed class SchemaNode {

    open class Type(
            val kqlType: KQLType,
            val properties : Map<String, SchemaNode.Property> = emptyMap(),
            val unionProperties: Map<String, SchemaNode.UnionProperty> = emptyMap()
    ) : SchemaNode()

    class ReturnType(
            val type: SchemaNode.Type,
            val isCollection : Boolean = false,
            val isNullable: Boolean = false,
            val areEntriesNullable : Boolean = false
    ) : Type(type.kqlType, type.properties, type.unionProperties)

    interface Branch

    abstract class SingleBranch(val returnType: SchemaNode.ReturnType) : Branch, SchemaNode()

    class UnionProperty (
            val kqlProperty: KQLProperty.Union,
            val returnTypes: List<SchemaNode.ReturnType>
    ) : Branch, SchemaNode()

    class Property(
            val kqlProperty: KQLProperty,
            returnType : SchemaNode.ReturnType,
            val transformation: Transformation<*,*>? = null
    ) : SingleBranch(returnType)

    abstract class Operation<T>(
            val kqlOperation: KQLOperation<T>,
            returnType: SchemaNode.ReturnType
    ) : SingleBranch(returnType)

    class Query<T>(
            val kqlQuery: KQLQuery<T>,
            returnType: SchemaNode.ReturnType
    ) : Operation<T>(kqlQuery, returnType)

    class Mutation<T>(
            val kqlMutation: KQLMutation<T>,
            returnType: SchemaNode.ReturnType
    ) : Operation<T>(kqlMutation, returnType)
}