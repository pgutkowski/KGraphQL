package com.github.pgutkowski.kgraphql.schema

import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.configuration.SchemaConfiguration
import com.github.pgutkowski.kgraphql.request.CachingDocumentParser
import com.github.pgutkowski.kgraphql.request.DocumentParser
import com.github.pgutkowski.kgraphql.request.VariablesJson
import com.github.pgutkowski.kgraphql.schema.execution.ParallelRequestExecutor
import com.github.pgutkowski.kgraphql.schema.execution.RequestExecutor
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.model.SchemaDefinition
import com.github.pgutkowski.kgraphql.schema.structure.SchemaStructure
import com.github.pgutkowski.kgraphql.schema.structure.TypeDefinitionProvider
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType

class DefaultSchema(
        internal val definition: SchemaDefinition,
        internal val configuration: SchemaConfiguration
) : Schema, TypeDefinitionProvider {

    companion object {
        const val OPERATION_NAME_PARAM = "operationName"
    }

    val structure = SchemaStructure.of(definition)

    val requestExecutor : RequestExecutor = ParallelRequestExecutor(this)

    /**
     * objects for request handling
     */
    private val documentParser = if(configuration.useCachingDocumentParser){
        CachingDocumentParser(configuration.documentParserCacheMaximumSize)
    } else {
        DocumentParser()
    }

    override fun execute(request: String, variables: String?): String {
        val parsedVariables = variables
                ?.let { VariablesJson.Defined(configuration.objectMapper, variables) }
                ?: VariablesJson.Empty()
        val operations = documentParser.parseDocument(request)

        when(operations.size){
            0 -> {
                throw RequestException("Must provide any operation")
            }
            1 -> {
                return requestExecutor.execute(structure.createExecutionPlan(operations.first()), parsedVariables)
            }
            else -> {
                if(operations.any { it.name == null }){
                    throw RequestException("anonymous operation must be the only defined operation")
                } else {
                    val executionPlans = operations.associate { it.name to structure.createExecutionPlan(it) }

                    val operationName = parsedVariables.get(String::class, String::class.starProjectedType, OPERATION_NAME_PARAM)
                            ?: throw RequestException("Must provide an operation name from: ${executionPlans.keys}")

                    val executionPlan = executionPlans[operationName]
                            ?: throw RequestException("Must provide an operation name from: ${executionPlans.keys}, found $operationName")

                    return requestExecutor.execute(executionPlan, parsedVariables)
                }
            }
        }
    }

    override fun typeByKClass(kClass: KClass<*>): KQLType? = typeByKType(kClass.starProjectedType)

    override fun typeByKType(kType: KType): KQLType? = structure.queryTypes[kType]?.kqlType

    override fun inputTypeByKClass(kClass: KClass<*>): KQLType? = inputTypeByKType(kClass.starProjectedType)

    override fun inputTypeByKType(kType: KType): KQLType? = structure.inputTypes[kType]?.kqlType
}