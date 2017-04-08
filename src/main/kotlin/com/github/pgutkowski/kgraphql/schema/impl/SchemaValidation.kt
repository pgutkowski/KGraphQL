package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.schema.SchemaException
import kotlin.reflect.KType
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

fun validateSchema(
        queries: ArrayList<KQLObject.Query<*>>,
        mutations: ArrayList<KQLObject.Mutation<*>>,

        types: ArrayList<KQLObject.Simple<*>>,
        inputs: ArrayList<KQLObject.Input<*>>,
        scalars: ArrayList<KQLObject.Scalar<*>>
) {
    fun validateType(kType: KType, errorMsg: String) {

        val kClass = kType.jvmErasure
        when (kClass) {
            String::class, Int::class, Float::class, Double::class -> {
                /** Do nothing, valid type */
            }
            Collection::class -> {
                kType.arguments.first().type?.jvmErasure?.isSuperclassOf(kClass) ?: throw SchemaException(errorMsg)
            }
            else -> {
                val isScalar = scalars.any { scalar ->
                    scalar.kClass.isSuperclassOf(kClass)
                }
                val isType = types.any { type ->
                    type.kClass.isSuperclassOf(kClass)
                }

                if (!isScalar && !isType) throw SchemaException(errorMsg)
            }
        }
    }

    fun <T> validateFunctionWrapper(query: FunctionWrapper<T>) {
        validateType(
                query.kFunction.returnType,
                "Class ${query.kFunction.returnType} declared as return type of $query is not registered in schema"
        )

        query.kFunction.valueParameters.forEach { parameter ->
            validateType(
                    parameter.type,
                    "Class ${parameter.type} declared as parameter type of $query is not registered in schema"
            )
        }
    }

    queries.forEach { query ->
        validateFunctionWrapper(query)
    }

    mutations.forEach { mutation ->
        validateFunctionWrapper(mutation)
    }
}

