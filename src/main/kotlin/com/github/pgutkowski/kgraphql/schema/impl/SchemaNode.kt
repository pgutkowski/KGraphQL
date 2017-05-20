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

    abstract class Branch(val returnType: SchemaNode.ReturnType) : SchemaNode()

    class UnionProperty(
            val kqlProperty: KQLProperty.Union,
            val returnTypes: Collection<SchemaNode.Type>
    ) : SchemaNode()

    class Property(
            val kqlProperty: KQLProperty,
            returnType : SchemaNode.ReturnType,
            val transformation: Transformation<*,*>? = null
    ) : Branch(returnType)

    abstract class Operation<T>(
            val kqlOperation: KQLOperation<T>,
            returnType: SchemaNode.ReturnType
    ) : Branch(returnType)

    class Query<T>(
            val kqlQuery: KQLQuery<T>,
            returnType: SchemaNode.ReturnType
    ) : Operation<T>(kqlQuery, returnType)

    class Mutation<T>(
            val kqlMutation: KQLMutation<T>,
            returnType: SchemaNode.ReturnType
    ) : Operation<T>(kqlMutation, returnType)
}