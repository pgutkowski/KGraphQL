package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.directive.Directive
import com.github.pgutkowski.kgraphql.schema.model.BaseKQLOperation
import com.github.pgutkowski.kgraphql.schema.model.KQLMutation
import com.github.pgutkowski.kgraphql.schema.model.KQLQuery
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.model.SchemaDefinition
import kotlin.reflect.full.starProjectedType


class SchemaStructureBuilder(model : SchemaDefinition) {

    val queries: List<KQLQuery<*>> = model.queries
    val mutations: List<KQLMutation<*>> = model.mutations
    val objects: List<KQLType.Object<*>> = model.objects
    val scalars: List<KQLType.Scalar<*>> = model.scalars
    val enums: List<KQLType.Enumeration<*>> = model.enums
    val unions: List<KQLType.Union> = model.unions
    val directives: List<Directive> = model.directives
    val inputObjects: List<KQLType.Input<*>> = model.inputObjects

    private val enumNodes = enums.associate { it.kClass.starProjectedType to transformEnum(it) }

    private val scalarNodes = scalars.associate { it.kClass.starProjectedType to transformScalar(it) }

    private val enumAndScalarNodes = enumNodes + scalarNodes

    fun build() : SchemaStructure {

        val queryTypesLinker = QueryStructureLinker(enumNodes, scalarNodes, objects)

        val inputTypesLinker = InputStructureLinker(enumNodes, scalarNodes, inputObjects)

        val queryNodes = queries
                .map { query -> SchemaNode.Query(query, queryTypesLinker.handleOperation(query)) }
                .associate { query -> query.kqlQuery.name to query }

        val mutationNodes = mutations
                .map { mutation -> SchemaNode.Mutation(mutation, queryTypesLinker.handleOperation(mutation))}
                .associate { mutation -> mutation.kqlMutation.name to mutation }

        objects.forEach { queryTypesLinker.linkType(it.kClass.starProjectedType) }

        val queryObjectTypes = queryTypesLinker.linkedTypes

        (inputObjects.map { it.kClass.starProjectedType } + queryTypesLinker.foundInputTypes).forEach { type ->
            inputTypesLinker.linkType(type)
        }

        queryTypesLinker.foundInputTypes.forEach {  }

        val inputObjectTypes = inputTypesLinker.linkedTypes

        val intersectedTypes = queryObjectTypes.keys.intersect(inputObjectTypes.keys)
        if(intersectedTypes.isNotEmpty()){
            throw SchemaException("Input object must be separate type in system. found common types $intersectedTypes")
        }

        return SchemaStructure (
                queryNodes,
                mutationNodes,
                queryObjectTypes.toMap() + enumAndScalarNodes.toMap(),
                inputObjectTypes.toMap() + enumAndScalarNodes.toMap(),
                directives.associate { it.name to it }
        )
    }

    fun transformEnum(enum : KQLType.Enumeration<*>) : SchemaNode.Type {
        return SchemaNode.Type(enum)
    }

    fun <T : Any>transformScalar(scalar : KQLType.Scalar<T>) : SchemaNode.Type {
        return SchemaNode.Type(scalar)
    }
}