package com.github.pgutkowski.kgraphql.schema.impl

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.request.Arguments
import com.github.pgutkowski.kgraphql.request.GraphNode
import com.github.pgutkowski.kgraphql.request.Request
import com.github.pgutkowski.kgraphql.request.RequestParser
import com.github.pgutkowski.kgraphql.result.Result
import com.github.pgutkowski.kgraphql.result.ResultSerializer
import com.github.pgutkowski.kgraphql.schema.Schema
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType

class DefaultSchema(
        val queries: ArrayList<KQLObject.Query<*>>,

        val mutations: ArrayList<KQLObject.Mutation<*>>,

        val types : ArrayList<KQLObject.Simple<*>>,

        val scalars: ArrayList<KQLObject.Scalar<*>>
) : Schema {

    companion object {
        val BUILT_IN_TYPES = arrayOf(String::class, Int::class, Double::class, Float::class, Boolean::class)

        private val BUILT_IN_TYPE_TRANSFORMATIONS = mapOf<KType, (String)->Any>(
                Int::class.starProjectedType to String::toInt,
                Double::class.starProjectedType to String::toDouble,
                Float::class.starProjectedType to String::toFloat,
                Boolean::class.starProjectedType to String::toBoolean
        )
    }

    val descriptor = SchemaDescriptor.forSchema(this)

    /**
     * KQLObjects stored in convenient data structures
     */
    private val queriesByName: Map<String, KQLObject.Query<*>> = queries.associate { it.name to it }

    private val mutationsByName: Map<String, KQLObject.Mutation<*>> = mutations.associate { it.name to it }

    /**
     * objects for request handling
     */
    private val requestParser = RequestParser { resolveActionType(it) }

    val objectMapper = jacksonObjectMapper()

    init {
        objectMapper.registerModule(
                SimpleModule("KQL result serializer").addSerializer(Result::class.java, ResultSerializer(this))
        )
    }

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
                descriptor.validateQueryGraph(parsedRequest.graph)
                for (query in parsedRequest.graph) {
                    data.put(query.aliasOrKey, invokeWithArgs(findQuery(query.key), extractArguments(query)))
                }
            }
            Request.Action.MUTATION -> {
                descriptor.validateMutationGraph(parsedRequest.graph)
                for (mutation in parsedRequest.graph) {
                    data.put(mutation.aliasOrKey, invokeWithArgs(findMutation(mutation.key), extractArguments(mutation)))
                }
            }
            else -> throw IllegalArgumentException("Not supported action: ${parsedRequest.action}")
        }
        return Result(parsedRequest, data, null)
    }

    fun <T> invokeWithArgs(functionWrapper: FunctionWrapper<T>, args: Arguments): Any? {
        if(functionWrapper.arity() != args.size){

            functionWrapper as KQLObject

            throw SyntaxException(
                "Mutation ${functionWrapper.name} does support arguments: ${functionWrapper.kFunction.parameters.map { it.name }}. found arguments: ${args.keys}"
            )
        }

        val transformedArgs : MutableList<Any?> = mutableListOf()
        functionWrapper.kFunction.parameters.forEach { parameter ->
            val value = args[parameter.name!!]
            if(value == null){
                if(!parameter.isOptional){
                    throw IllegalArgumentException("${functionWrapper.kFunction.name} argument ${parameter.name} is not optional, value cannot be null")
                } else {
                    transformedArgs.add(null)
                }
            } else if(value is String) {

                val transformedValue : Any = when(parameter.type){
                    String::class.starProjectedType -> value.dropQuotes()
                    in BUILT_IN_TYPE_TRANSFORMATIONS -> {
                        try {
                            BUILT_IN_TYPE_TRANSFORMATIONS[parameter.type]!!.invoke(value)
                        } catch (e : Exception){
                            throw SyntaxException("argument \'${value.dropQuotes()}\' is not value of type: ${parameter.type}")
                        }
                    }
                    else -> {
                        throw UnsupportedOperationException("Not supported yet")
                    }
                }

                transformedArgs.add(transformedValue)
            } else {
                throw SyntaxException("Non string arguments are not supported yet")
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

    fun String.dropQuotes() : String = if(startsWith('\"') && endsWith('\"')) drop(1).dropLast(1) else this

    /**
     * @param name - name of mutation function to find
     */
    fun findMutation(name: String): KQLObject.Mutation<*> {
        return mutationsByName[name] ?: throw SyntaxException("Mutation: $name is not supported by this schema")
    }

    /**
     * @param name - name of query function to find
     */
    fun findQuery(name: String): KQLObject.Query<*> {
        return queriesByName[name] ?: throw SyntaxException("Query: $name is not supported by this schema")
    }

    /**
     * returns Scalar for passed instance, or null if not found
     */
    fun findScalarByInstance(any : Any) : KQLObject.Scalar<*>? {
        return scalars.find{it.kClass.isInstance(any)}
    }

    fun resolveActionType(token: String): Request.Action {
        if (queries.any { it.name.equals(token, true) }) return Request.Action.QUERY
        if (mutations.any { it.name.equals(token, true) }) return Request.Action.MUTATION
        throw IllegalArgumentException("Cannot infer request type for name $token")
    }
}