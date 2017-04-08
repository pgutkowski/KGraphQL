package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.request.Arguments
import com.github.pgutkowski.kgraphql.request.Graph
import com.github.pgutkowski.kgraphql.request.Request
import kotlin.reflect.KClass
import kotlin.reflect.full.valueParameters

/**
 * simpleTypes are stored as map of name -> KQLType, of speed up lookup time. Data duplication is bad, but this is necessary
 */
open class DefaultSchemaStructure(
        private val queries: ArrayList<KQLObject.Query<*>>,
        private val mutations: ArrayList<KQLObject.Mutation<*>>,

        private val types : ArrayList<KQLObject.Simple<*>>,
        private val inputs: ArrayList<KQLObject.Input<*>>,
        private val scalars: ArrayList<KQLObject.Scalar<*>>
) {

    private val queriesByName: Map<String, KQLObject.Query<*>> = queries.associate { it.name to it }

    private val mutationsByName: Map<String, KQLObject.Mutation<*>> = mutations.associate { it.name to it }

    private val typesByClass: Map<KClass<*>, KQLObject.Simple<*>> = types.associate { it.kClass to it }

    private val inputsByClass: Map<KClass<*>, KQLObject.Input<*>> = inputs.associate { it.kClass to it }

    private val descriptor: Graph = createDescriptor()

    private fun createDescriptor(): Graph {
        return Graph()
    }

    fun findMutation(name: String, args: Arguments): KQLObject.Mutation<*> {
        val mutation = mutationsByName[name] ?: throw SyntaxException("Mutation: $name is not supported by this schema")
        if(mutation.kFunction.valueParameters.size != args.size) throw SyntaxException("Mutation function $name with arguments: ${args.keys} not found")
        return mutation
    }

    fun findQuery(name: String, args: Arguments): KQLObject.Query<*> {
        val query = queriesByName[name] ?: throw SyntaxException("Query: $name is not supported by this schema")
        if(query.kFunction.valueParameters.size != args.size) throw SyntaxException("Resolver for query $name with arguments: ${args.keys} not found")
        return query
    }

    //lookup scalar by instance of class
    fun findScalarByInstance(any : Any) : KQLObject.Scalar<*>? {
        return scalars.find{it.kClass.isInstance(any)}
    }

    fun resolveActionType(token: String): Request.Action {
        if (queriesByName.values.any { it.name.equals(token, true) }) return Request.Action.QUERY
        if (mutationsByName.values.any { it.name.equals(token, true) }) return Request.Action.MUTATION
        throw IllegalArgumentException("Cannot infer request type for name $token")
    }
}