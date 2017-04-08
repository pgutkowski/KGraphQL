package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.SyntaxException
import com.github.pgutkowski.kql.annotation.method.Mutation
import com.github.pgutkowski.kql.annotation.method.Query
import com.github.pgutkowski.kql.request.Arguments
import com.github.pgutkowski.kql.schema.impl.function.MutationFunction
import com.github.pgutkowski.kql.schema.impl.function.QueryFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

/**
 * types are stored as map of name -> KQLType, of speed up lookup time. Data duplication is bad, but this is necessary
 */
open class DefaultSchemaStructure(
        val types: HashMap<String, MutableList<KQLObject>>,
        val queries: ArrayList<KQLObject.QueryField<*>>,
        val mutations: ArrayList<KQLObject.Mutation>,
        val inputs: ArrayList<KQLObject.Input<*>>,
        val scalars: ArrayList<KQLObject.Scalar<*>>
) {
    private val queryResolvingFunctions: Map<String, List<QueryFunction>> = buildQueryFunctionsMap()

    private val mutationResolvingFunctions: Map<String, List<MutationFunction>> = buildMutationFunctionsMap()

    private fun buildQueryFunctionsMap(): Map<String, List<QueryFunction>> {
        val result: MutableMap<String, MutableList<QueryFunction>> = mutableMapOf()
        queries.forEach { query ->
            query.resolvers.forEach { resolver ->
                resolver.javaClass.kotlin.memberFunctions
                        .filter { it.findAnnotation<Query>() != null }
                        .forEach { function ->
                            val list = result.getOrPut(query.name, { mutableListOf<QueryFunction>() })
                            list.add(QueryFunction(resolver, function))
                        }
            }
        }
        return result
    }

    private fun buildMutationFunctionsMap(): Map<String, List<MutationFunction>> {
        val result: MutableMap<String, MutableList<MutationFunction>> = mutableMapOf()
        mutations.forEach { mutation ->
            mutation.resolver.javaClass.kotlin.memberFunctions
                    .filter { it.findAnnotation<Mutation>() != null }
                    .forEach { function ->
                        val list = result.getOrPut(function.name, { mutableListOf<MutationFunction>() })
                        list.add(MutationFunction(mutation.resolver, function))

                    }
        }
        return result
    }

    protected fun findMutationFunction(mutation: String, args: Arguments): MutationFunction {
        val functions = mutationResolvingFunctions[mutation] ?: throw SyntaxException("Mutation: $mutation is not supported by this schema")
        val queryFunction = functions.find { it.arity == args.size } ?: throw SyntaxException("Mutation function $mutation with arguments: ${args.keys} not found")
        return queryFunction
    }

    protected fun findQueryFunction(query: String, args: Arguments): QueryFunction {
        val functions = queryResolvingFunctions[query] ?: throw SyntaxException("Query: $query is not supported by this schema")
        val queryFunction = functions.find { it.arity == args.size } ?: throw SyntaxException("No query function with arguments: $args found")
        return queryFunction
    }

}