package com.github.pgutkowski.kgraphql.schema.impl

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.graph.GraphNode
import com.github.pgutkowski.kgraphql.request.*
import com.github.pgutkowski.kgraphql.result.Result
import com.github.pgutkowski.kgraphql.result.ResultSerializer
import com.github.pgutkowski.kgraphql.schema.Schema
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

class DefaultSchema(
        val queries: List<KQLObject.Query<*>>,
        val mutations: List<KQLObject.Mutation<*>>,
        val types : List<KQLObject.Simple<*>>,
        val scalars: List<KQLObject.Scalar<*>>,
        val enums : List<KQLObject.Enumeration<*>>
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

    override val descriptor = SchemaDescriptor.forSchema(this)

    /**
     * KQLObjects stored in convenient data structures
     */
    private val queriesByName: Map<String, KQLObject.Query<*>> = queries.associate { it.name to it }

    private val mutationsByName: Map<String, KQLObject.Mutation<*>> = mutations.associate { it.name to it }

    private val enumsByType = enums.associate { it.kClass.createType() to it }

    val objectMapper = jacksonObjectMapper()

    /**
     * objects for request handling
     */
    private val requestParser = RequestParser { resolveActionType(it) }

    init {
        objectMapper.registerModule(
                SimpleModule("KQL result serializer").addSerializer(Result::class.java, ResultSerializer(this))
        )
    }

    override fun handleRequest(request: String, variables: String?): String {
        try {
            return objectMapper.writeValueAsString(createResult(request, variables))
        } catch(e: Exception) {
            return "{\"errors\" : { \"message\": \"Caught ${e.javaClass.canonicalName}: ${e.message}\"}}"
        }
    }

    /**
     * this method is only fetching data
     */
    fun createResult(request: String, variablesJson: String? = null): Result {
        val jsonNode = if(variablesJson != null) objectMapper.readTree(variablesJson) else null
        val variables = Variables(objectMapper, jsonNode)
        val parsedRequest = requestParser.parse(request)

        val data: MutableMap<String, Any?> = mutableMapOf()
        val intersectedSchema = descriptor.intersect(parsedRequest.graph)
        when (parsedRequest.action) {
            Request.Action.QUERY -> {
                for (query in parsedRequest.graph) {
                    data.put(query.aliasOrKey, invokeFunWrapper(findQuery(query.key), extractArguments(query), variables))
                }
            }
            Request.Action.MUTATION -> {
                for (mutation in parsedRequest.graph) {
                    data.put(mutation.aliasOrKey, invokeFunWrapper(findMutation(mutation.key), extractArguments(mutation), variables))
                }
            }
            else -> throw IllegalArgumentException("Not supported action: ${parsedRequest.action}")
        }
        return Result(parsedRequest, intersectedSchema, data, null)
    }

    fun invokeFunWrapper(functionWrapper: FunctionWrapper<*>, args: Arguments, variables: Variables): Any? {
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
                val transformedValue: Any? = transformValue(parameter, value, variables)
                transformedArgs.add(transformedValue)
            } else {
                throw SyntaxException("Non string arguments are not supported yet")
            }

        }

        return functionWrapper.invoke(*transformedArgs.toTypedArray())
    }

    private fun transformValue(parameter: KParameter, value: String, variables: Variables): Any? {

        if(value.startsWith("$")){
            return variables.getVariable(parameter.type.jvmErasure, value.substring(1))
        }

        return when (parameter.type) {

            /*simple string*/
            String::class.starProjectedType -> value.dropQuotes()

            /*One of other build in types, string needs to be parsed*/
            in BUILT_IN_TYPE_TRANSFORMATIONS -> {
                try {
                    BUILT_IN_TYPE_TRANSFORMATIONS[parameter.type]!!.invoke(value)
                } catch (e: Exception) {
                    throw SyntaxException("argument \'${value.dropQuotes()}\' is not value of type: ${parameter.type}")
                }
            }

            /* check if enum */
            in enumsByType.keys -> {
                enumsByType[parameter.type]?.values?.find { it.name == value }
            }

            else -> {
                throw UnsupportedOperationException("Not supported yet")
            }
        }
    }

    fun extractArguments(graphNode: GraphNode): Arguments {
        return graphNode.arguments ?: Arguments()
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