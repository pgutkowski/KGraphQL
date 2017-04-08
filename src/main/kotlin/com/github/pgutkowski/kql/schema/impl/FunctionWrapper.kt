@file:Suppress("UNCHECKED_CAST")

package com.github.pgutkowski.kql.schema.impl

import kotlin.reflect.KFunction
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.reflect


/**
 * lots of boilerplate here, because kotlin-reflect doesn't support invoking lambdas, local and anonymous functions yet
 */
interface FunctionWrapper <T>{

    companion object {
        fun <T> on (function : () -> T) = ArityZero(function)
        fun <T, R> on (function : (R) -> T) = ArityOne(function)
        fun <T, R, E> on (function : (R, E) -> T) = ArityTwo(function)
        fun <T, R, E, W> on (function : (R, E, W) -> T) = ArityThree(function)
        fun <T, R, E, W, Q> on (function : (R, E, W, Q) -> T) = ArityFour(function)
    }

    val kFunction: KFunction<T>

    fun invoke(vararg args: Any?) : T?

    fun arity() : Int

    class ArityZero<T>(val implementation : ()-> T ) : FunctionWrapper<T>{
        override val kFunction: KFunction<T>
            get() = implementation.reflect()!!

        override fun arity(): Int = 0

        override fun invoke(vararg args: Any?): T? {
            if(args.isNotEmpty()){
                throw IllegalArgumentException("This function does not accept arguments")
            } else {
                return implementation()
            }
        }
    }

    class ArityOne<T, R>(val implementation : (R)-> T ) : FunctionWrapper<T>{
        override val kFunction: KFunction<T>
            get() = implementation.reflect()!!

        override fun arity(): Int = 1

        override fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }

    class ArityTwo<T, R, E>(val implementation : (R, E)-> T ) : FunctionWrapper<T>{
        override val kFunction: KFunction<T>
            get() = implementation.reflect()!!

        override fun arity(): Int = 2

        override fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R, args[1] as E)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }

    class ArityThree<T, R, E, W>(val implementation : (R, E, W)-> T ) : FunctionWrapper<T>{
        override val kFunction: KFunction<T>
            get() = implementation.reflect()!!

        override fun arity(): Int = 3

        override fun invoke(vararg args: Any?): T? {
            if(args.size == arity()){
                return implementation(args[0] as R, args[1] as E, args[2] as W)
            } else {
                throw IllegalArgumentException("This function needs exactly ${arity()} arguments")
            }
        }
    }

    class ArityFour<T, R, E, W, Q>(val implementation : (R, E, W, Q)-> T ) : FunctionWrapper<T>{
        override val kFunction: KFunction<T>
            get() = implementation.reflect()!!

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