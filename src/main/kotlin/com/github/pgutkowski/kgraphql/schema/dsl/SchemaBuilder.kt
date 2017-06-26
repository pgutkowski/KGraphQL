package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.introspection.__SchemaProxy
import com.github.pgutkowski.kgraphql.schema.model.KQLEnumValue
import com.github.pgutkowski.kgraphql.schema.model.KQLMutation
import com.github.pgutkowski.kgraphql.schema.model.KQLQuery
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.model.MutableSchemaDefinition
import kotlin.reflect.KClass

/**
 * SchemaBuilder exposes rich DSL to setup GraphQL schema
 */
class SchemaBuilder(private val init: SchemaBuilder.() -> Unit) {

    private val model = MutableSchemaDefinition()

    private var configuration = SchemaConfigurationDSL()

    fun build(): Schema {
        init()
        val proxy = __SchemaProxy()
        val schema = DefaultSchema(model.toSchemaModel(proxy), configuration.build())
        proxy.proxiedSchema = schema
        return schema
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
        model.addScalar(KQLType.Scalar(scalar.name, kClass, scalar.getCoercion(), scalar.description))
    }

    inline fun <reified T : Any> stringScalar(noinline block: ScalarDSL<T, String>.() -> Unit) {
        stringScalar(T::class, block)
    }

    fun <T : Any>intScalar(kClass: KClass<T>, block: ScalarDSL<T, Int>.() -> Unit){
        val scalar = IntScalarDSL(kClass, block)
        model.addScalar(KQLType.Scalar(scalar.name, kClass, scalar.getCoercion(), scalar.description))
    }

    inline fun <reified T : Any> intScalar(noinline block: ScalarDSL<T, Int>.() -> Unit) {
        intScalar(T::class, block)
    }

    fun <T : Any>floatScalar(kClass: KClass<T>, block: ScalarDSL<T, Double>.() -> Unit){
        val scalar = DoubleScalarDSL(kClass, block)
        model.addScalar(KQLType.Scalar(scalar.name, kClass, scalar.getCoercion(), scalar.description))
    }

    inline fun <reified T : Any> floatScalar(noinline block: ScalarDSL<T, Double>.() -> Unit) {
        floatScalar(T::class, block)
    }

    fun <T : Any>booleanScalar(kClass: KClass<T>, block: ScalarDSL<T, Boolean>.() -> Unit){
        val scalar = BooleanScalarDSL(kClass, block)
        model.addScalar(KQLType.Scalar(scalar.name, kClass, scalar.getCoercion(), scalar.description))
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
                KQLEnumValue(value, valueDSL.description, valueDSL.isDeprecated, valueDSL.deprecationReason)
            } ?: KQLEnumValue(value)
        }

        model.addEnum(KQLType.Enumeration(type.name, kClass, kqlEnumValues, type.description))
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
        model.addUnion(KQLType.Union(name, union.possibleTypes, union.description))
        return TypeID(name)
    }

    //================================================================================
    // INPUT
    //================================================================================

    fun <T : Any>inputType(kClass: KClass<T>, block : InputTypeDSL<T>.() -> Unit) {
        val input = InputTypeDSL(kClass, block)
        model.addInputObject(KQLType.Input(input.name, kClass, input.description))
    }

    inline fun <reified T : Any> inputType(noinline block : InputTypeDSL<T>.() -> Unit) {
        inputType(T::class, block)
    }

    inline fun <reified T : Any> inputType() {
        inputType(T::class, {})
    }
}