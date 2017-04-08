package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.SyntaxException
import com.github.pgutkowski.kql.request.Arguments
import kotlin.reflect.full.valueParameters

/**
 * simpleTypes are stored as map of name -> KQLType, of speed up lookup time. Data duplication is bad, but this is necessary
 */
open class DefaultSchemaStructure(
        queries: ArrayList<KQLObject.Query<*>>,
        mutations: ArrayList<KQLObject.Mutation<*>>,

        val simpleTypes : ArrayList<KQLObject.Simple<*>>,
        val inputs: ArrayList<KQLObject.Input<*>>,
        val scalars: ArrayList<KQLObject.Scalar<*>>
) {
    protected val queries: Map<String, KQLObject.Query<*>> = queries.associate { it.name to it }

    protected val mutations: Map<String, KQLObject.Mutation<*>> = mutations.associate { it.name to it }

    protected fun findMutationFunction(name: String, args: Arguments): KQLObject.Mutation<*> {
        val mutation = mutations[name] ?: throw SyntaxException("Mutation: $name is not supported by this schema")
        if(mutation.kFunction.valueParameters.size != args.size) throw SyntaxException("Mutation function $name with arguments: ${args.keys} not found")
        return mutation
    }

    protected fun findQueryFunction(name: String, args: Arguments): KQLObject.Query<*> {
        val query = queries[name] ?: throw SyntaxException("Query: $name is not supported by this schema")
        if(query.kFunction.valueParameters.size != args.size) throw SyntaxException("Resolver for query $name with arguments: ${args.keys} not found")
        return query
    }

}