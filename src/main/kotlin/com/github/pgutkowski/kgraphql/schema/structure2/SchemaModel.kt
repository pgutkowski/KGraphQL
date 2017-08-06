package com.github.pgutkowski.kgraphql.schema.structure2

import com.github.pgutkowski.kgraphql.schema.directive.Directive
import com.github.pgutkowski.kgraphql.schema.introspection.__Schema
import com.github.pgutkowski.kgraphql.schema.introspection.__Type
import kotlin.reflect.KClass


data class SchemaModel (
        val query: Type,
        val mutation: Type,
        val enums: Map<KClass<out Enum<*>>, Type.Enum<out Enum<*>>>,
        val scalars : Map<KClass<*>, Type.Scalar<*>>,
        val unions : List<Type.Union>,
        val allTypes : Map<KClass<*>, Type>,
        val queryTypes: Map<KClass<*>, Type>,
        val inputTypes: Map<KClass<*>, Type>,
        override val directives: List<Directive>
) : __Schema {

    val allTypesByName = allTypes.values.associate { it.name to it }

    val queryTypesByName = queryTypes.values.associate { it.name to it }

    val inputTypesByName = inputTypes.values.associate { it.name to it }

    override val types: List<__Type> = allTypes.values.toList()
            .filterNot { it.name?.startsWith("__") ?: false }
            //workaround on the fact that Double and Float are treated as GraphQL Float
            .filterNot { it is Type.Scalar<*> && it.kClass == Float::class }

    override val queryType: __Type = query

    override val mutationType: __Type? = mutation

    override val subscriptionType: __Type? = null

    override fun findTypeByName(name: String): __Type? = allTypesByName[name]
}