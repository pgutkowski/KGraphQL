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
            val kotlinProperties: Map<KProperty1<T, *>, KQLProperty.Kotlin<T, *>>,
            val extensionProperties : List<KQLProperty.Function<*>>,
            val unionProperties : List<KQLProperty.Union>,
            val transformations : List<Transformation<T, *>>,
            description : String?
    ) : BaseKQLType(name, description), Kotlin<T> {

        constructor (
                name : String, kClass: KClass<T>
        ) : this(name, kClass, emptyMap(), emptyList(), emptyList(), emptyList(), null)

        fun isIgnored(property: KProperty1<*, *>): Boolean = kotlinProperties[property]?.isIgnored ?: false
    }

    /**
     * duplicates object fields, but that's intentional: [Object] and [Interface] should not inherit from each other
     * to avoid bugs in type checks
     */
    class Interface<T : Any> (
            name : String,
            override val kClass: KClass<T>,
            val kotlinProperties: Map<KProperty1<T, *>, KQLProperty.Kotlin<T, *>>,
            val extensionProperties : List<KQLProperty.Function<*>>,
            val unionProperties : List<KQLProperty.Union>,
            val transformations : List<Transformation<T, *>>,
            description : String?
    ) : BaseKQLType(name, description), Kotlin<T> {

        constructor (
                name : String, kClass: KClass<T>
        ) : this(name, kClass, emptyMap(), emptyList(), emptyList(), emptyList(), null)

        fun isIgnored(property: KProperty1<*, *>): Boolean = kotlinProperties[property]?.isIgnored ?: false
    }


    class Input<T : Any>(
            name : String,
            override val kClass: KClass<T>,
            description: String?
    ) : BaseKQLType(name, description), Kotlin<T>

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
            val values: List<KQLEnumValue<T>>,
            description : String? = null
    ) : BaseKQLType(name, description), Kotlin<T>
}