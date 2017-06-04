package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.schema.directive.Directive
import com.github.pgutkowski.kgraphql.schema.model.KQLMutation
import com.github.pgutkowski.kgraphql.schema.model.KQLQuery
import com.github.pgutkowski.kgraphql.schema.model.KQLType

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
)