package com.github.pgutkowski.kgraphql.schema.dsl

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.model.EnumValueDef
import com.github.pgutkowski.kgraphql.schema.model.TypeDef
import com.github.pgutkowski.kgraphql.schema.model.MutableSchemaDefinition
import com.github.pgutkowski.kgraphql.schema.structure2.SchemaCompilation
import kotlin.reflect.KClass

/**
 * SchemaBuilder exposes rich DSL to setup GraphQL schema
 */
class SchemaBuilder<Context : Any>(private val init: SchemaBuilder<Context>.() -> Unit) {

    private val model = MutableSchemaDefinition()

    private var configuration = SchemaConfigurationDSL()

    fun build(): Schema {
        init()
        return SchemaCompilation(configuration.build(), model.toSchemaDefinition()).perform()
    }

    fun configure(block: SchemaConfigurationDSL.() -> Unit){
        configuration.update(block)
    }

    //================================================================================
    // OPERATIONS
    //================================================================================

    fun query(name : String, init: QueryOrMutationDSL.() -> Unit){
        model.addQuery(QueryOrMutationDSL(name, init).toKQLQuery())
    }

    fun mutation(name : String, init: QueryOrMutationDSL.() -> Unit){
        model.addMutation(QueryOrMutationDSL(name, init).toKQLMutation())
    }

    //================================================================================
    // SCALAR
    //================================================================================

    fun <T : Any>stringScalar(kClass: KClass<T>, block: ScalarDSL<T, String>.() -> Unit){
        val scalar = StringScalarDSL(kClass, block)
        configuration.appendMapper(scalar, kClass)
        model.addScalar(TypeDef.Scalar(scalar.name, kClass, scalar.createCoercion(), scalar.description))
    }

    inline fun <reified T : Any> stringScalar(noinline block: ScalarDSL<T, String>.() -> Unit) {
        stringScalar(T::class, block)
    }

    fun <T : Any>intScalar(kClass: KClass<T>, block: ScalarDSL<T, Int>.() -> Unit){
        val scalar = IntScalarDSL(kClass, block)
        configuration.appendMapper(scalar, kClass)
        model.addScalar(TypeDef.Scalar(scalar.name, kClass, scalar.createCoercion(), scalar.description))
    }

    inline fun <reified T : Any> intScalar(noinline block: ScalarDSL<T, Int>.() -> Unit) {
        intScalar(T::class, block)
    }

    fun <T : Any>floatScalar(kClass: KClass<T>, block: ScalarDSL<T, Double>.() -> Unit){
        val scalar = DoubleScalarDSL(kClass, block)
        configuration.appendMapper(scalar, kClass)
        model.addScalar(TypeDef.Scalar(scalar.name, kClass, scalar.createCoercion(), scalar.description))
    }

    inline fun <reified T : Any> floatScalar(noinline block: ScalarDSL<T, Double>.() -> Unit) {
        floatScalar(T::class, block)
    }

    fun <T : Any>longScalar(kClass: KClass<T>, block: ScalarDSL<T, Long>.() -> Unit){
        val scalar = LongScalarDSL(kClass, block)
        configuration.appendMapper(scalar, kClass)
        model.addScalar(TypeDef.Scalar(scalar.name, kClass, scalar.createCoercion(), scalar.description))
    }

    inline fun <reified T : Any>longScalar(noinline block: ScalarDSL<T, Long>.() -> Unit) {
        longScalar(T::class, block)
    }

    fun <T : Any>booleanScalar(kClass: KClass<T>, block: ScalarDSL<T, Boolean>.() -> Unit){
        val scalar = BooleanScalarDSL(kClass, block)
        configuration.appendMapper(scalar, kClass)
        model.addScalar(TypeDef.Scalar(scalar.name, kClass, scalar.createCoercion(), scalar.description))
    }

    inline fun <reified T : Any> booleanScalar(noinline block: ScalarDSL<T, Boolean>.() -> Unit) {
        booleanScalar(T::class, block)
    }

    //================================================================================
    // TYPE
    //================================================================================

    fun <T : Any>type(kClass: KClass<T>, block: TypeDSL<T>.() -> Unit){
        val type = TypeDSL(model.unionsMonitor, kClass, block)
        model.addObject(type.toKQLObject())
    }

    inline fun <reified T : Any> type(noinline block: TypeDSL<T>.() -> Unit) {
        type(T::class, block)
    }

    inline fun <reified T : Any> type() {
        type(T::class, {})
    }

    //================================================================================
    // ENUM
    //================================================================================

    fun <T : Enum<T>>enum(kClass: KClass<T>, enumValues : Array<T>, block: (EnumDSL<T>.() -> Unit)? = null){
        val type = EnumDSL(kClass, block)

        val kqlEnumValues = enumValues.map { value ->
            type.valueDefinitions[value]?.let { valueDSL ->
                EnumValueDef (
                        value = value,
                        description = valueDSL.description,
                        isDeprecated = valueDSL.isDeprecated,
                        deprecationReason = valueDSL.deprecationReason
                )
            } ?: EnumValueDef(value)
        }

        model.addEnum(TypeDef.Enumeration(type.name, kClass, kqlEnumValues, type.description))
    }

    inline fun <reified T : Enum<T>> enum(noinline block: (EnumDSL<T>.() -> Unit)? = null) {
        val enumValues = enumValues<T>()
        if(enumValues.isEmpty()){
            throw SchemaException("Enum of type ${T::class} must have at least one value")
        } else {
            enum(T::class, enumValues<T>(), block)
        }
    }

    //================================================================================
    // UNION
    //================================================================================

    fun unionType(name : String, block : UnionTypeDSL.() -> Unit) : TypeID {
        val union = UnionTypeDSL(block)
        model.addUnion(TypeDef.Union(name, union.possibleTypes, union.description))
        return TypeID(name)
    }

    //================================================================================
    // INPUT
    //================================================================================

    fun <T : Any>inputType(kClass: KClass<T>, block : InputTypeDSL<T>.() -> Unit) {
        val input = InputTypeDSL(kClass, block)
        model.addInputObject(TypeDef.Input(input.name, kClass, input.description))
    }

    inline fun <reified T : Any> inputType(noinline block : InputTypeDSL<T>.() -> Unit) {
        inputType(T::class, block)
    }

    inline fun <reified T : Any> inputType() {
        inputType(T::class, {})
    }
}

inline fun <T: Any, reified Raw: Any> SchemaConfigurationDSL.appendMapper(scalar: ScalarDSL<T, Raw>, kClass: KClass<T>) {
    objectMapper.registerModule(SimpleModule().addDeserializer(kClass.java, object : UsesDeserializer<T>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): T? {
            return scalar
                .createCoercion()
                .deserialize(p.readValueAs(Raw::class.java))
        }
    }))
}

open class UsesDeserializer<T>(vc: Class<*>? = null) : StdDeserializer<T>(vc) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): T? = TODO("Implement")
}