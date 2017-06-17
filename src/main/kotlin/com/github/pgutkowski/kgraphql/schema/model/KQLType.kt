package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.schema.scalar.ScalarCoercion
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KQLType {

    val name : String

    val description : String?

    abstract class BaseKQLType(name : String, override val description: String?) : KQLType, KQLObject(name)

    interface Kotlin<T : Any> : KQLType {
        val kClass : KClass<T>
    }

    class Object<T : Any> (
            name : String,
            override val kClass: KClass<T>,
            val ignoredProperties : List<KProperty1<T, *>>,
            val extensionProperties : List<KQLProperty.Function<*>>,
            val unionProperties : List<KQLProperty.Union>,
            val transformations : List<Transformation<T, *>>,
            description : String?
    ) : BaseKQLType(name, description), Kotlin<T> {

        constructor(name : String, kClass: KClass<T>) : this(name, kClass, emptyList(), emptyList(), emptyList(), emptyList(), null)

        fun isIgnored(property: KProperty1<*, *>): Boolean = ignoredProperties.any { it == property }
    }

    class Scalar<T : Any> (
            name : String,
            override val kClass: KClass<T>,
            val coercion: ScalarCoercion<T, *>,
            description : String?
    ) : BaseKQLType(name, description), Kotlin<T>

    //To avoid circular dependencies etc. union type members are resolved in runtime
    class Union (
            name : String,
            val members: Set<KClass<*>>,
            description : String?
    ) : BaseKQLType(name, description)

    class Enumeration<T : Enum<T>> (
            name: String,
            override val kClass: KClass<T>,
            val values: Array<T>,
            description : String?
    ) : BaseKQLType(name, description), Kotlin<T>
}