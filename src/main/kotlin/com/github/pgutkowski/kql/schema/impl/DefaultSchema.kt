package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.annotation.method.ResolvingFunction
import com.github.pgutkowski.kql.request.*
import com.github.pgutkowski.kql.request.result.Errors
import com.github.pgutkowski.kql.request.result.DataGraphBuilder
import com.github.pgutkowski.kql.request.result.Result
import com.github.pgutkowski.kql.resolve.QueryResolver
import com.github.pgutkowski.kql.schema.Schema
import javax.naming.OperationNotSupportedException
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

/**
 * types are stored as map of name -> KQLType, to speed up lookup time. Data duplication is bad, but this is necessary
 */
class DefaultSchema(
        val types: HashMap<String, MutableList<KQLObject>>,
        val queries: ArrayList<KQLObject.QueryField<*>>,
        val mutations: ArrayList<KQLObject.Mutation>,
        val inputs: ArrayList<KQLObject.Input<*>>,
        val scalars: ArrayList<KQLObject.Scalar<*, *>>
) : Schema {

    private val queryResolvingFunctions: Map<String, List<QueryFunction>> = findQueryFunctions()

    private val requestParser = RequestParser { resolveActionType(it) }

    private val graphBuilder = DataGraphBuilder(this)

    override fun handleRequest(request: String): Result {
        try {
            val parsedRequest = requestParser.parse(request)
            val data = Graph()
            when(parsedRequest.action){
                ParsedRequest.Action.QUERY -> {
                    for((query, value) in parsedRequest.graph){
                        val functions = queryResolvingFunctions[query] ?: throw SyntaxException("Query: $query is not supported by this schema")
                        val queryFunction = functions.find { it.function.parameters.size == 1 } ?: throw SyntaxException("No query function with arguments: '' found")
                        data.put(query, graphBuilder.from(queryFunction.invoke()!!, if(value is Graph) value else null))
                    }
                }
                ParsedRequest.Action.MUTATION -> {
                    throw OperationNotSupportedException("dude")
                }
                else -> throw IllegalArgumentException("Not supported action: ${parsedRequest.action}")
            }
            return Result(data, null)
        } catch (e : Exception){
            return Result(null, Errors("${e.javaClass}: ${e.message}"))
        }
    }

    private fun findQueryFunctions() : Map<String, List<QueryFunction>>{
        var result : MutableMap<String, MutableList<QueryFunction>> = mutableMapOf()
        queries.forEach{ query ->
            query.resolvers.forEach { resolver ->
                resolver.javaClass.kotlin.memberFunctions
                        .filter { it.findAnnotation<ResolvingFunction>() != null }
                        .forEach { function ->
                            val list = result.getOrPut(query.name, { mutableListOf<QueryFunction>() })
                            list.add(QueryFunction(resolver, function))
                        }
            }
        }
        return result
    }

    fun resolveActionType(token : String) : ParsedRequest.Action{
        if (queries.any { it.name.equals(token, true) }) return ParsedRequest.Action.QUERY
        if (mutations.flatMap { it.functions }.any { it.name.equals(token, true)}) return ParsedRequest.Action.MUTATION
        throw IllegalArgumentException("Cannot infer request type for name $token")
    }

    private data class QueryFunction(val resolver : QueryResolver<*>, val function : KFunction<*>){
        fun invoke() : Any? {
            return function.call(resolver)
        }
    }
}