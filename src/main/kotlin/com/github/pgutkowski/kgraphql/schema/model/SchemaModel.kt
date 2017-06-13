package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.schema.directive.Directive

/**
 * [SchemaModel] represents unstructured schema components
 */
data class SchemaModel (
        val objects: List<KQLType.Object<*>>,
        val queries: List<KQLQuery<*>>,
        val scalars: List<KQLType.Scalar<*>>,
        val mutations: List<KQLMutation<*>>,
        val enums: List<KQLType.Enumeration<*>>,
        val unions: List<KQLType.Union>,
        val directives: List<Directive>
) {
    val allTypesByKClass =
            objects.associate { it.kClass to it } +
            scalars.associate { it.kClass to it } +
            enums.associate { it.kClass to it }
}