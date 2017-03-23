package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.support.ClassSupport
import kotlin.reflect.KClass


sealed class KQLType<T : Any>(
        val name : String,
        val kClass: KClass<T>,
        val classSupports: List <ClassSupport<*>>
) {
    class Simple<T : Any>(name : String, kClass: KClass<T>, classSupports: List <ClassSupport<*>>) : KQLType<T>(name, kClass, classSupports)

    class Mutation<T : Any>(name : String, kClass: KClass<T>, classSupports: List <ClassSupport<*>>) : KQLType<T>(name, kClass, classSupports)

    class Query<T : Any>(name : String, kClass: KClass<T>, classSupports: List <ClassSupport<T>>) : KQLType<T>(name, kClass, classSupports)

    class Input<T : Any>(name : String, kClass: KClass<T>, classSupports: List <ClassSupport<*>>) : KQLType<T>(name, kClass, classSupports)

    class Scalar<T : Any>(name : String, kClass: KClass<T>, classSupports: List <ClassSupport<*>>) : KQLType<T>(name, kClass, classSupports)

    class Interface<T : Any>(name : String, kClass: KClass<T>) : KQLType<T>(name, kClass, emptyList())
}