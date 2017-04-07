package com.github.pgutkowski.kql.schema.impl

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.pgutkowski.kql.request.Arguments
import com.github.pgutkowski.kql.request.GraphNode
import com.github.pgutkowski.kql.request.Request
import com.github.pgutkowski.kql.request.RequestParser
import com.github.pgutkowski.kql.result.Result
import com.github.pgutkowski.kql.result.ResultSerializer
import com.github.pgutkowski.kql.schema.Schema
import com.github.pgutkowski.kql.schema.impl.function.FunctionWrapper
import kotlin.reflect.full.starProjectedType

class DefaultSchema(
        types: HashMap<String, MutableList<KQLObject>>,
        queries: ArrayList<KQLObject.QueryField<*>>,
        mutations: ArrayList<KQLObject.Mutation>,
        inputs: ArrayList<KQLObject.Input<*>>,
        scalars: ArrayList<KQLObject.Scalar<*>>
) : Schema, DefaultSchemaStructure(
        types, queries, mutations, inputs, scalars
) {
    private val requestParser = RequestParser { resolveActionType(it) }

    val objectMapper = jacksonObjectMapper()

    init {
        objectMapper.registerModule(
                SimpleModule("KQL result serializer").addSerializer(Result::class.java, ResultSerializer(this))
        )
    }

    //TODO: fix error handling on stage of serializing
    override fun handleRequest(request: String): String {
        try {
            return objectMapper.writeValueAsString(createResult(request))
        } catch(e: Exception) {
            return "{\"errors\" : { \"message\": \"Caught ${e.javaClass.canonicalName}: ${e.message}\"}}"
        }
    }

    /**
     * this method is only fetching data
     */
    fun createResult(request: String): Result {
        val parsedRequest = requestParser.parse(request)
        val data: MutableMap<String, Any?> = mutableMapOf()
        when (parsedRequest.action) {
            Request.Action.QUERY -> {
                for (query in parsedRequest.graph.map { it.key }) {
                    val queryFunction = findQueryFunction(query, Arguments())
                    data.put(query, queryFunction.invoke())
                }
            }
            Request.Action.MUTATION -> {
                for (mutation in parsedRequest.graph) {
                    val args = extractArguments(mutation)
                    val mutationFunction = findMutationFunction(mutation.key, args)
                    data.put(mutation.key, invokeWithArgs(mutationFunction, args))
                }
            }
            else -> throw IllegalArgumentException("Not supported action: ${parsedRequest.action}")
        }
        return Result(parsedRequest, data, null)
    }

    fun <T> invokeWithArgs(functionWrapper: FunctionWrapper<T>, args: Arguments): Any? {
        val transformedArgs : MutableList<Any?> = mutableListOf()
        //drop first because it is receiver, instance of declaring class
        functionWrapper.function.parameters.drop(1).forEachIndexed { i, parameter ->
            val value = args[parameter.name!!]
            if(value == null){
                if(!parameter.isOptional){
                    throw IllegalArgumentException("${functionWrapper.function.name} argument ${parameter.name} is not optional, value cannot be null")
                } else {
                    transformedArgs.add(null)
                }
            } else {
                val transformedValue : Any = when(parameter.type){
                    Int::class.starProjectedType -> value.toInt()
                    else -> value
                }

                transformedArgs.add(transformedValue)
            }

        }

        return functionWrapper.invoke(*transformedArgs.toTypedArray())
    }

    fun extractArguments(graphNode: GraphNode): Arguments {
        if(graphNode is GraphNode.ToArguments){
            return graphNode.arguments
        } else {
            return Arguments()
        }
    }

    fun resolveActionType(token: String): Request.Action {
        if (queries.any { it.name.equals(token, true) }) return Request.Action.QUERY
        if (mutations.flatMap { it.functions }.any { it.name.equals(token, true) }) return Request.Action.MUTATION
        throw IllegalArgumentException("Cannot infer request type for name $token")
    }

}