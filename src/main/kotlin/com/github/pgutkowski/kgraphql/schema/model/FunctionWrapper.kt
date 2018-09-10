@file:Suppress("UNCHECKED_CAST")

package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.structure2.validateName
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlin.reflect.KFunction
import kotlin.reflect.KType
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
        fun <T> on (function : () -> T) : FunctionWrapper<T>
                = FunctionWrapper.ArityZero(function)

        fun <T, R> on (function : (R) -> T)
                = FunctionWrapper.ArityOne(function, false)

        fun <T, R> on (function : (R) -> T, hasReceiver: Boolean = false)
                = FunctionWrapper.ArityOne(function, hasReceiver)

        fun <T, R, E> on (function : (R, E) -> T, hasReceiver: Boolean = false)
                = FunctionWrapper.ArityTwo(function, hasReceiver)

        fun <T, R, E, W> on (function : (R, E, W) -> T, hasReceiver: Boolean = false)
                = FunctionWrapper.ArityThree(function, hasReceiver)

        fun <T, R, E, W, Q> on (function : (R, E, W, Q) -> T, hasReceiver: Boolean = false)
                = FunctionWrapper.ArityFour(function, hasReceiver)

        fun <T, R, E, W, Q, A> on (function : (R, E, W, Q, A) -> T, hasReceiver: Boolean = false)
                = FunctionWrapper.ArityFive(function, hasReceiver)

        fun <T, R, E, W, Q, A, S> on (function : (R, E, W, Q, A, S) -> T, hasReceiver: Boolean = false)
                = FunctionWrapper.AritySix(function, hasReceiver)

        fun <T> onSuspend (function : suspend () -> T) : FunctionWrapper<T>
            = FunctionWrapper.ArityZeroSuspend(function)

        fun <T, R> onSuspend (function : suspend (R) -> T)
            = FunctionWrapper.ArityOneSuspend(function, false)

        fun <T, R> onSuspend (function : suspend (R) -> T, hasReceiver: Boolean = false)
            = FunctionWrapper.ArityOneSuspend(function, hasReceiver)

        fun <T, R, E> onSuspend (function : suspend (R, E) -> T, hasReceiver: Boolean = false)
            = FunctionWrapper.ArityTwoSuspend(function, hasReceiver)

        fun <T, R, E, W> onSuspend (function : suspend (R, E, W) -> T, hasReceiver: Boolean = false)
            = FunctionWrapper.ArityThreeSuspend(function, hasReceiver)

        fun <T, R, E, W, Q> onSuspend (function : suspend (R, E, W, Q) -> T, hasReceiver: Boolean = false)
            = FunctionWrapper.ArityFourSuspend(function, hasReceiver)

        fun <T, R, E, W, Q, A> onSuspend (function : suspend (R, E, W, Q, A) -> T, hasReceiver: Boolean = false)
            = FunctionWrapper.ArityFiveSuspend(function, hasReceiver)

        fun <T, R, E, W, Q, A, S> onSuspend (function : suspend (R, E, W, Q, A, S) -> T, hasReceiver: Boolean = false)
            = FunctionWrapper.AritySixSuspend(function, hasReceiver)

    }

    val kFunction: KFunction<T>

    suspend fun suspendInvoke(vararg args: Any?) : T?

    fun invoke(vararg args: Any?) : T?

    fun arity() : Int

    /**
     * denotes whether function is called with receiver argument.
     * Receiver argument in GraphQL is somewhat similar to kotlin receivers:
     * its value is passed by framework, usually it is parent of function property with [FunctionWrapper]
     * Receiver argument is omitted in schema, and cannot be stated in query document.
     */
    val hasReceiver : Boolean

    val argumentsDescriptor : Map<String, KType>

    abstract class Base<T> : FunctionWrapper<T>{
        private fun createArgumentsDescriptor(): Map<String, KType> {
            return valueParameters().associate { parameter ->
                val parameterName = parameter.name
                        ?: throw SchemaException("Cannot handle nameless argument on function: $kFunction")

                validateName(parameterName)
                parameterName to parameter.type
            }
        }

        override val argumentsDescriptor: Map<String, KType> by lazy { createArgumentsDescriptor() }

        override fun invoke(vararg args: Any?) : T? = runBlocking {
            suspendInvoke(*args)
        }

        override suspend fun suspendInvoke(vararg args: Any?) : T? {
            return invoke(*args)
        }
    }

    /**
     * returns list of function parameters without receiver
     */
    fun valueParameters(): List<kotlin.reflect.KParameter> {
        return kFunction.valueParameters.let {
            if(hasReceiver) it.drop(1) else it
        }
    }

    class ArityZero<T>(val implementation : ()-> T, override val hasReceiver: Boolean = false ) : Base<T>() {
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

    class ArityZeroSuspend<T>(val implementation : suspend ()-> T, override val hasReceiver: Boolean = false ) : Base<T>() {
        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }
        override fun arity(): Int = 0

        override suspend fun suspendInvoke(vararg args: Any?): T? {
            if(args.isNotEmpty()){
                throw IllegalArgumentException("This function does not accept arguments")
            } else {
                return implementation()
            }
        }
    }

    class ArityOne<T, R>(
            val implementation : (R)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {

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

    class ArityOneSuspend<T, R>(
        val implementation : suspend (R)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {

        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }

        override fun arity(): Int = 1

        override suspend fun suspendInvoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }

    class ArityTwo<T, R, E>(
            val implementation : (R, E)-> T,
            override val hasReceiver: Boolean
    ) : Base<T>() {

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

    class ArityTwoSuspend<T, R, E>(
        val implementation : suspend (R, E)-> T,
        override val hasReceiver: Boolean
    ) : Base<T>() {

        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }

        override fun arity(): Int = 2

        override suspend fun suspendInvoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R, args[1] as E)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }

    class ArityThree<T, R, E, W>(
            val implementation : (R, E, W)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {

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

    class ArityThreeSuspend<T, R, E, W>(
        val implementation : suspend (R, E, W)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {

        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }

        override fun arity(): Int = 3

        override suspend fun suspendInvoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R, args[1] as E, args[2] as W)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }

    class ArityFour<T, R, E, W, Q>(
            val implementation : (R, E, W, Q)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {

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

    class ArityFourSuspend<T, R, E, W, Q>(
        val implementation : suspend (R, E, W, Q)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {

        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }

        override fun arity(): Int = 4

        override suspend fun suspendInvoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }

    class ArityFive<T, R, E, W, Q, A>(
            val implementation : (R, E, W, Q, A)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {

        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }

        override fun arity(): Int = 5

        override fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }

    class ArityFiveSuspend<T, R, E, W, Q, A>(
        val implementation : suspend (R, E, W, Q, A)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {

        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }

        override fun arity(): Int = 5

        override suspend fun suspendInvoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }

    class AritySix<T, R, E, W, Q, A, S>(
            val implementation : (R, E, W, Q, A, S)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {

        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }

        override fun arity(): Int = 6

        override fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }

    class AritySixSuspend<T, R, E, W, Q, A, S>(
        val implementation : suspend (R, E, W, Q, A, S)-> T, override val hasReceiver: Boolean
    ) : Base<T>() {

        override val kFunction: KFunction<T> by lazy { implementation.reflect()!! }
        override fun arity(): Int = 6

        override suspend fun suspendInvoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }
}