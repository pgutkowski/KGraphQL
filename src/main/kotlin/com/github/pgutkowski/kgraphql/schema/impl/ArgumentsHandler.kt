package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.dropQuotes
import com.github.pgutkowski.kgraphql.isNotNullable
import com.github.pgutkowski.kgraphql.isNullable
import com.github.pgutkowski.kgraphql.request.Arguments
import com.github.pgutkowski.kgraphql.request.Variables
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import kotlin.reflect.KParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure


internal class ArgumentsHandler(val schema : SchemaModel) {

    private val enumsByType = schema.enums.associate { it.kClass.createType() to it }

    private val scalarsByType = schema.scalars.associate { it.kClass.createType() to it }

    /**
     * transform arguments accepts custom handler for case when invoker wants to add special argument handling logic
     * see
     */
    fun <T>transformArguments (
            functionWrapper: FunctionWrapper<T>,
            args: Arguments?,
            variables: Variables
    ) : List<Any?>{
        val parameters = functionWrapper.valueParameters()

        return parameters.map { parameter ->
            val value = args?.get(parameter.name)

            when {
                value == null && parameter.isNullable() -> null
                value == null && parameter.isNotNullable() -> {
                    throw IllegalArgumentException("${functionWrapper.kFunction.name} argument ${parameter.name} is not optional, value cannot be null")
                }
                value is String -> transformValue(parameter, value, variables)
                else -> throw SyntaxException("Non string arguments are not supported yet")
            }
        }
    }

    private fun transformValue(parameter: KParameter, value: String, variables: Variables): Any? {

        if(value.startsWith("$")){
            return variables.get(parameter.type.jvmErasure, value.substring(1))
        } else {
            val literalValue = value.dropQuotes()
            //drop nullability on lookup type
            val kType = parameter.type.withNullability(false)
            return when (kType) {
                String::class.starProjectedType -> literalValue
                in enumsByType.keys -> enumsByType[kType]?.values?.find { it.name == literalValue }
                in scalarsByType.keys ->{
                    transformScalar(scalarsByType[kType]!!, literalValue)
                }
                else -> {
                    throw UnsupportedOperationException("Not supported yet")
                }
            }
        }
    }

    private fun <T : Any>transformScalar(support : KQLType.Scalar<T>, value : String): T {
        try {
            return support.scalarSupport.serialize(value)
        } catch (e: Exception){
            throw SyntaxException("argument '$value' is not value of type ${support.name}")
        }
    }
}