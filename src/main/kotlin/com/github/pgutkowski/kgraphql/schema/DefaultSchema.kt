package com.github.pgutkowski.kgraphql.schema

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.configuration.SchemaConfiguration
import com.github.pgutkowski.kgraphql.request.DocumentParser
import com.github.pgutkowski.kgraphql.request.VariablesJson
import com.github.pgutkowski.kgraphql.schema.execution.RequestExecutor
import com.github.pgutkowski.kgraphql.schema.model.SchemaModel
import com.github.pgutkowski.kgraphql.schema.structure.SchemaStructure

class DefaultSchema(internal val model : SchemaModel, internal val configuration: SchemaConfiguration) : Schema {

    companion object {
        val OPERATION_NAME_PARAM = "operationName"
    }

    val structure = SchemaStructure.of(model)

    val requestExecutor = RequestExecutor(this)

    /**
     * objects for request handling
     */
    private val documentParser = DocumentParser()

    override fun execute(request: String, variables: String?): String {
        val parsedVariables = variables
                ?.let { VariablesJson.Defined(configuration.objectMapper, variables) }
                ?: VariablesJson.Empty()
        val operations = documentParser.parseDocument(request)

        when(operations.size){
            0 -> {
                throw SyntaxException("Must provide any operation")
            }
            1 -> {
                return requestExecutor.execute(structure.createExecutionPlan(operations.first()), parsedVariables)
            }
            else -> {
                if(operations.any { it.name == null }){
                    throw SyntaxException("anonymous operation must be the only defined operation")
                } else {
                    val executionPlans = operations.associate { it.name to structure.createExecutionPlan(it) }
                    val operationName = parsedVariables.get(String::class, OPERATION_NAME_PARAM)
                            ?: throw SyntaxException("Must provide an operation name from: ${executionPlans.keys}")
                    val executionPlan = executionPlans[operationName]
                            ?: throw SyntaxException("Must provide an operation name from: ${executionPlans.keys}, found $operationName")
                    return requestExecutor.execute(executionPlan, parsedVariables)
                }
            }
        }
    }
}