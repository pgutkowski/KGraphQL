@file:Suppress("UNCHECKED_CAST")

package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.ValidationException
import com.github.pgutkowski.kgraphql.isNullable
import com.github.pgutkowski.kgraphql.request.Arguments
import kotlin.reflect.KFunction
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.reflect


/**
 * FunctionWrapper is common interface for classes storing functions registered in schema by server code.
 * Only up to 4 arguments are supported, because kotlin-reflect doesn't support
 * invoking lambdas, local and anonymous functions yet, making implementation severely limited.
 */
interface FunctionWrapper <T>{
    //lots of boilerplate here, because kotlin-reflect doesn't support invoking lambdas, local and anonymous functions yet
    companion object {
        fun <T> on (function : () -> T) : FunctionWrapper<T> = FunctionWrapper.ArityZero(function)

        fun <T, R> on (function : (R) -> T, hasReceiver: Boolean = false) = FunctionWrapper.ArityOne(function, hasReceiver)

        fun <T, R, E> on (function : (R, E) -> T, hasReceiver: Boolean = false) = FunctionWrapper.ArityTwo(function, hasReceiver)

        fun <T, R, E, W> on (function : (R, E, W) -> T, hasReceiver: Boolean = false) = FunctionWrapper.ArityThree(function, hasReceiver)

        fun <T, R, E, W, Q> on (function : (R, E, W, Q) -> T, hasReceiver: Boolean = false) = FunctionWrapper.ArityFour(function, hasReceiver)
    }

    val kFunction: KFunction<T>

    fun invoke(vararg args: Any?) : T?

    fun arity() : Int

    /**
     * denotes whether function is called with receiver argument.
     * Receiver argument in GraphQL is somewhat similar to kotlin receivers:
     * its value is passed by framework, usually it is parent of function property with [FunctionWrapper]
     * Receiver argument is omitted in schema, and cannot be stated in query document.
     */
    val hasReceiver : Boolean

    fun validateArguments(args: Arguments?) : List<ValidationException> {
        val exceptions = mutableListOf<ValidationException>()

        val parameterNames = kFunction.parameters.map { it.name }
        val invalidArguments = args?.filterKeys { it !in parameterNames }

        if(invalidArguments != null && invalidArguments.isNotEmpty()){
            invalidArguments.forEach { name, _ -> exceptions.add(ValidationException("Invalid argument $name")) }
        }

        kFunction.parameters.forEachIndexed { index, kParameter ->
            val value = args?.get(kParameter.name)
            if(value != null || (kParameter.isNullable() || (hasReceiver && index == 0))){
                //is valid
            } else {
                exceptions.add(ValidationException("Missing value for non-nullable argument ${kParameter.name}"))
            }
        }
        return exceptions
    }

    /**
     * returns list of function parameters without receiver
     */
    fun valueParameters(): List<kotlin.reflect.KParameter> {
        return kFunction.valueParameters.let {
            if(hasReceiver) it.drop(1) else it
        }
    }

    class ArityZero<T>(val implementation : ()-> T, override val hasReceiver: Boolean = false ) : FunctionWrapper<T> {
        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }

        override fun arity(): Int = 0

        override fun invoke(vararg args: Any?): T? {
            if(args.isNotEmpty()){
                throw IllegalArgumentException("This function does not accept arguments")
            } else {
                return implementation()
            }
        }
    }

    class ArityOne<T, R>(val implementation : (R)-> T, override val hasReceiver: Boolean) : FunctionWrapper<T> {
        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }

        override fun arity(): Int = 1

        override fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }

    class ArityTwo<T, R, E>(val implementation : (R, E)-> T, override val hasReceiver: Boolean ) : FunctionWrapper<T> {
        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }

        override fun arity(): Int = 2

        override fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R, args[1] as E)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }

    class ArityThree<T, R, E, W>(val implementation : (R, E, W)-> T, override val hasReceiver: Boolean ) : FunctionWrapper<T> {
        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }

        override fun arity(): Int = 3

        override fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R, args[1] as E, args[2] as W)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }

    class ArityFour<T, R, E, W, Q>(val implementation : (R, E, W, Q)-> T, override val hasReceiver: Boolean ) : FunctionWrapper<T> {
        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }

        override fun arity(): Int = 4

        override fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }
}