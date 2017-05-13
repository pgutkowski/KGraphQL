package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.schema.model.*


sealed class SchemaNode {

    open class Type(
            val kqlType: KQLType,
            val properties : Map<String, SchemaNode.Property>
    ) : SchemaNode(), KQLType by kqlType

    class ReturnType(
            val type: SchemaNode.Type,
            val isCollection : Boolean = false,
            val isNullable: Boolean = false,
            val areEntriesNullable : Boolean = false
    ) : Type(type.kqlType, type.properties)

    abstract class Branch(val returnType: SchemaNode.ReturnType) : SchemaNode()

    class Property(
            val kqlProperty: KQLProperty,
            returnType : SchemaNode.ReturnType
    ) : Branch(returnType), KQLProperty by kqlProperty

    abstract class Operation<T>(
            val kqlOperation: KQLOperation<T>,
            returnType: SchemaNode.ReturnType
    ) : Branch(returnType), KQLOperation<T> by kqlOperation

    class Query<T>(
            val kqlQuery: KQLQuery<T>,
            returnType: SchemaNode.ReturnType
    ) : Operation<T>(kqlQuery, returnType)

    class Mutation<T>(
            val kqlMutation: KQLMutation<T>,
            returnType: SchemaNode.ReturnType
    ) : Operation<T>(kqlMutation, returnType)
}