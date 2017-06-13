package com.github.pgutkowski.kgraphql.schema.execution

import com.github.pgutkowski.kgraphql.ExecutionException
import com.github.pgutkowski.kgraphql.request.Arguments
import com.github.pgutkowski.kgraphql.request.Variables
import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper

/**
 * 
 */
internal class FunctionInvoker(private val argumentsHandler: ArgumentsHandler) {

    //TODO: Refactor to avoid using so many arguments
    fun <T>invokeFunWrapper(funName: String,
                            functionWrapper: FunctionWrapper<T>,
                            receiver: Any?,
                            args: Arguments?,
                            variables: Variables): T? {
        val transformedArgs = argumentsHandler.transformArguments(funName, functionWrapper, args, variables)

        try {
            if(receiver != null){
                return functionWrapper.invoke(receiver, *transformedArgs.toTypedArray())
            } else {
                return functionWrapper.invoke(*transformedArgs.toTypedArray())
            }
        } catch (e: Exception){
            throw ExecutionException("Failed to invoke function $funName", e)
        }
    }
}