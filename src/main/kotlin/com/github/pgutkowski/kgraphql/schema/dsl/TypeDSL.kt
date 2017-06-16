package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.Transformation
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.defaultKQLTypeName
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1


open class TypeDSL<T : Any>(private val supportedUnions: Collection<KQLType.Union>, val kClass: KClass<T>, block: TypeDSL<T>.() -> Unit) : ItemDSL() {

    var name = kClass.defaultKQLTypeName()

    internal val ignoredProperties = mutableSetOf<KProperty1<T, *>>()

    internal val transformationProperties = mutableSetOf<Transformation<T, *>>()

    internal val extensionProperties = mutableSetOf<KQLProperty.Function<*>>()

    internal val unionProperties = mutableSetOf<KQLProperty.Union>()

    infix fun ignore(kProperty: KProperty1<T, *>){
        ignoredProperties.add(kProperty)
    }

    fun <R, E> transformation(kProperty: KProperty1<T, R>, function: (R, E) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W> transformation(kProperty: KProperty1<T, R>, function: (R, E, W) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W, Q> transformation(kProperty: KProperty1<T, R>, function: (R, E, W, Q) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R> property(name : String, block : PropertyDSL<T, R>.() -> Unit){
        val it = PropertyDSL(block)
        extensionProperties.add(KQLProperty.Function(name, it.functionWrapper))
    }

    fun unionProperty(name : String, block : UnionPropertyDSL<T>.() -> Unit){
        val unionProperty = UnionPropertyDSL(block)
        val union = supportedUnions.find { unionProperty.returnType.typeID.equals(it.name, true) }
                ?: throw SchemaException("Union Type: ${unionProperty.returnType.typeID} does not exist")
        unionProperties.add(KQLProperty.Union(name, unionProperty.functionWrapper, union))
    }

    init {
        block()
    }

    fun toKQLObject() : KQLType.Object<T> {
        return KQLType.Object(
                name,
                kClass,
                ignoredProperties.toList(),
                extensionProperties.toList(),
                unionProperties.toList(),
                transformationProperties.toList(),
                description
        )
    }
}