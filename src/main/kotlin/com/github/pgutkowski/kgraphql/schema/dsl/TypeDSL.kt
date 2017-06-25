package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.defaultKQLTypeName
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.model.Transformation
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1


open class TypeDSL<T : Any>(private val supportedUnions: Collection<KQLType.Union>, val kClass: KClass<T>, block: TypeDSL<T>.() -> Unit) : ItemDSL() {

    var name = kClass.defaultKQLTypeName()

    internal val transformationProperties = mutableSetOf<Transformation<T, *>>()

    internal val extensionProperties = mutableSetOf<KQLProperty.Function<*>>()

    internal val unionProperties = mutableSetOf<KQLProperty.Union>()

    internal val describedKotlinProperties = mutableMapOf<KProperty1<T, *>, KQLProperty.Kotlin<T, *>>()

    fun <R, E> transformation(kProperty: KProperty1<T, R>, function: (R, E) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W> transformation(kProperty: KProperty1<T, R>, function: (R, E, W) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W, Q> transformation(kProperty: KProperty1<T, R>, function: (R, E, W, Q) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun property(kProperty: KProperty1<T, *>, block : KotlinPropertyDSL<T>.() -> Unit){
        val dsl = KotlinPropertyDSL(kProperty, block)
        describedKotlinProperties[kProperty] = dsl.toKQLProperty()
    }

    fun <R> property(name : String, block : PropertyDSL<T, R>.() -> Unit){
        val dsl = PropertyDSL(name, block)
        extensionProperties.add(dsl.toKQL())
    }

    fun <R> KProperty1<T, R>.describe(block : KotlinPropertyDSL<T>.() -> Unit){
        property(this, block)
    }

    fun <R> KProperty1<T, R>.ignore(){
        describedKotlinProperties[this] = KQLProperty.Kotlin(kProperty = this, isIgnored = true)
    }

    fun unionProperty(name : String, block : UnionPropertyDSL<T>.() -> Unit){
        val property = UnionPropertyDSL(name, block)
        val union = supportedUnions.find { property.returnType.typeID.equals(it.name, true) }
                ?: throw SchemaException("Union Type: ${property.returnType.typeID} does not exist")

        unionProperties.add(property.toKQL(union))
    }

    init {
        block()
    }

    fun toKQLObject() : KQLType.Object<T> {
        return KQLType.Object(
                name,
                kClass,
                describedKotlinProperties.toMap(),
                extensionProperties.toList(),
                unionProperties.toList(),
                transformationProperties.toList(),
                description
        )
    }
}