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
        val objects: List<KQLType.Object<*>>,
        val queries: List<KQLQuery<*>>,
        val scalars: List<KQLType.Scalar<*>>,
        val mutations: List<KQLMutation<*>>,
        val enums: List<KQLType.Enumeration<*>>,
        val unions: List<KQLType.Union>,
        val directives: List<Directive>,
        val inputObjects: List<KQLType.Input<*>>
)