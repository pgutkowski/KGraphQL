package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.model.KQLMutation
import com.github.pgutkowski.kgraphql.schema.model.KQLQuery
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.model.MutableSchemaModel
import kotlin.reflect.KClass


class SchemaBuilder(private val init: SchemaBuilder.() -> Unit) {

    private val model = MutableSchemaModel()

    private var configuration = SchemaConfigurationDSL()

    fun build(): Schema {
        init()
        return DefaultSchema(model.toSchemaModel(), configuration.build())
    }

    fun configure(block: SchemaConfigurationDSL.() -> Unit){
        configuration.update(block)
    }

    fun query(name : String, init: QueryOrMutationDSL.() -> Unit){
        val wrapperDSL = QueryOrMutationDSL(init)
        model.addQuery(KQLQuery(name, wrapperDSL.functionWrapper, wrapperDSL.description))
    }

    fun mutation(name : String, init: QueryOrMutationDSL.() -> Unit){
        val wrapperDSL = QueryOrMutationDSL(init)
        model.addMutation(KQLMutation(name, wrapperDSL.functionWrapper, wrapperDSL.description))
    }

    fun <T : Any>supportedScalar(kClass: KClass<T>, block: SupportedScalarDSL<T>.() -> Unit){
        val scalar = SupportedScalarDSL(kClass, block)
        val support = scalar.support ?: throw SchemaException("Please specify scalar support object")

        model.addScalar(KQLType.Scalar(scalar.name, kClass, support, scalar.description))
    }

    fun <T : Any>scalar(kClass: KClass<T>, block: ScalarDSL<T>.() -> Unit){
        val scalar = ScalarDSL(kClass, block)

        if(scalar.missingSupportFunction()) throw SchemaException("Please specify scalar support functions")

        val support : ScalarSupport<T> = object : ScalarSupport<T> {
            override fun serialize(input: String): T = scalar.serialize!!(input)
            override fun deserialize(input: T): String = scalar.deserialize!!(input)
            override fun validate(input: String): Boolean = scalar.validate!!(input)
        }

        model.addScalar(KQLType.Scalar(scalar.name, kClass, support, scalar.description))
    }

    private fun <T : Any> ScalarDSL<T>.missingSupportFunction(): Boolean {
        return serialize == null || deserialize == null || validate == null
    }

    fun <T : Any>type(kClass: KClass<T>, block: TypeDSL<T>.() -> Unit){
        val type = TypeDSL(model.unionsMonitor, kClass, block)
        model.addObject(type.toKQLObject())
    }

    fun <T : Enum<T>>enum(kClass: KClass<T>, enumValues : Array<T>, block: (EnumDSL<T>.() -> Unit)? = null){
        val type = EnumDSL(kClass, block)
        model.addEnum(KQLType.Enumeration(type.name, kClass, enumValues, type.description))
    }

    fun unionType(name : String, block : UnionTypeDSL.() -> Unit) : TypeID {
        val union = UnionTypeDSL(block)
        model.addUnion(KQLType.Union(name, union.possibleTypes, union.description))
        return TypeID(name)
    }
}

inline fun <reified T : Any> SchemaBuilder.supportedScalar(noinline block: SupportedScalarDSL<T>.() -> Unit) {
    supportedScalar(T::class, block)
}

inline fun <reified T : Any> SchemaBuilder.scalar(noinline block: ScalarDSL<T>.() -> Unit) {
    scalar(T::class, block)
}

inline fun <reified T : Any> SchemaBuilder.type(noinline block: TypeDSL<T>.() -> Unit) {
    type(T::class, block)
}

inline fun <reified T : Any> SchemaBuilder.type() {
    type(T::class, {})
}

inline fun <reified T : Enum<T>> SchemaBuilder.enum(noinline block: (EnumDSL<T>.() -> Unit)? = null) {
    val enumValues = enumValues<T>()
    if(enumValues.isEmpty()){
        throw SchemaException("Enum of type ${T::class} must have at least one value")
    } else {
        enum(T::class, enumValues<T>(), block)
    }
}