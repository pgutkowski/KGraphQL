package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.schema.directive.Directive

/**
 * [SchemaDefinition] represents unstructured schema components
 *
 * [SchemaDefinition] does not contain all nodes in schema, only these,
 * which have been directly declared via [SchemaBuilder].
 *
 * [SchemaStructure] contains full schema tree, with all types
 */
data class SchemaDefinition(
        val objects: List<TypeDef.Object<*>>,
        val queries: List<QueryDef<*>>,
        val scalars: List<TypeDef.Scalar<*>>,
        val mutations: List<MutationDef<*>>,
        val enums: List<TypeDef.Enumeration<*>>,
        val unions: List<TypeDef.Union>,
        val directives: List<Directive.Partial>,
        val inputObjects: List<TypeDef.Input<*>>
)