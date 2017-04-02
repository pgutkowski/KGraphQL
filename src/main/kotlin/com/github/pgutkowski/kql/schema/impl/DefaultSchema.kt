package com.github.pgutkowski.kql.schema.impl

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.pgutkowski.kql.Graph
import com.github.pgutkowski.kql.SyntaxException
import com.github.pgutkowski.kql.annotation.method.ResolvingFunction
import com.github.pgutkowski.kql.request.Request
import com.github.pgutkowski.kql.request.RequestParser
import com.github.pgutkowski.kql.resolve.QueryResolver
import com.github.pgutkowski.kql.result.Result
import com.github.pgutkowski.kql.result.ResultSerializer
import com.github.pgutkowski.kql.schema.Schema
import javax.naming.OperationNotSupportedException
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.valueParameters

/**
 * types are stored as map of name -> KQLType, to speed up lookup time. Data duplication is bad, but this is necessary
 */
class DefaultSchema(
        val types: HashMap<String, MutableList<KQLObject>>,
        val queries: ArrayList<KQLObject.QueryField<*>>,
        val mutations: ArrayList<KQLObject.Mutation>,
        val inputs: ArrayList<KQLObject.Input<*>>,
        val scalars: ArrayList<KQLObject.Scalar<*>>
) : Schema {

    private val queryResolvingFunctions: Map<String, List<QueryFunction>> = buildQueryFunctionsMap()

    private val requestParser = RequestParser { resolveActionType(it) }

    val objectMapper = jacksonObjectMapper()

    init {
        objectMapper.registerModule(
                SimpleModule("KQL result serializer").addSerializer(Result::class.java, ResultSerializer(this))
        )
    }

    //TODO: fix error handling on stage of serializing
    override fun handleRequest(request: String): String {
        try{
            return objectMapper.writeValueAsString(createResult(request))
        } catch(e : Exception){
            return "{\"errors\" : { \"message\": \"Caught ${e.javaClass.canonicalName}: ${e.message}\"}}"
        }
    }

    /**
     * this method is only fetching data
     */
    fun createResult(request: String): Result {
            val parsedRequest = requestParser.parse(request)
            val data = Graph()
            when(parsedRequest.action){
                Request.Action.QUERY -> {
                    for((query, value) in parsedRequest.graph){
                        val queryFunction = findQueryFunction(query)
                        data.put(query, queryFunction.invoke())
                    }
                }
                Request.Action.MUTATION -> {
                    throw OperationNotSupportedException("Mutations are not supported yet")
                }
                else -> throw IllegalArgumentException("Not supported action: ${parsedRequest.action}")
            }
            return Result(parsedRequest, data, null)
    }

    private fun findQueryFunction(query: String): QueryFunction {
        val functions = queryResolvingFunctions[query] ?: throw SyntaxException("Query: $query is not supported by this schema")
        val queryFunction = functions.find { it.arity == 0 } ?: throw SyntaxException("No query function with arguments: '' found")
        return queryFunction
    }

    private fun buildQueryFunctionsMap() : Map<String, List<QueryFunction>>{
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

    fun resolveActionType(token : String) : Request.Action{
        if (queries.any { it.name.equals(token, true) }) return Request.Action.QUERY
        if (mutations.flatMap { it.functions }.any { it.name.equals(token, true)}) return Request.Action.MUTATION
        throw IllegalArgumentException("Cannot infer request type for name $token")
    }

    private data class QueryFunction(val resolver : QueryResolver<*>, val function : KFunction<*>){
        fun invoke() : Any? {
            return function.call(resolver)
        }

        /**
         * arity is number of arguments beside instance
         */
        val arity = function.valueParameters.size
    }
}