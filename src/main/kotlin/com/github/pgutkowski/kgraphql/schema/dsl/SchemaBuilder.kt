package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.impl.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.impl.KQLObject
import kotlin.reflect.KClass


class SchemaBuilder(private val init: SchemaBuilder.() -> Unit) {

    fun build(): Schema {
        init()
        return DefaultSchema(queries, mutations, simpleTypes, scalars, enums)
    }

    private val simpleTypes = arrayListOf<KQLObject.Object<*>>()

    private val queries = arrayListOf<KQLObject.Query<*>>()

    private val scalars = arrayListOf<KQLObject.Scalar<*>>()

    private val mutations = arrayListOf<KQLObject.Mutation<*>>()

    private val enums = arrayListOf<KQLObject.Enumeration<*>>()

    fun query(init: OperationDSL.() -> Unit){
        val wrapperDSL = OperationDSL(init)
        queries.add(KQLObject.Query(wrapperDSL.name, wrapperDSL.functionWrapper, wrapperDSL.description))
    }

    fun mutation(init: OperationDSL.() -> Unit){
        val wrapperDSL = OperationDSL(init)
        mutations.add(KQLObject.Mutation(wrapperDSL.name, wrapperDSL.functionWrapper, wrapperDSL.description))
    }

    fun <T : Any>scalar(kClass: KClass<T>, block: ScalarDSL<T>.() -> Unit){
        val scalar = ScalarDSL(kClass, block)

        val support : ScalarSupport<T>
        when {
            scalar.support != null && scalar.hasSupportFunctions() -> {
                throw IllegalArgumentException(
                        "Please specify either support object OR support functions(serialize, deserialize, validate), NOT BOTH"
                )
            }
            scalar.support != null -> support = scalar.support!!
            scalar.hasSupportFunctions() -> {
                support = object : ScalarSupport<T> {
                    override fun serialize(input: String): T = scalar.serialize!!(input)
                    override fun deserialize(input: T): String = scalar.deserialize!!(input)
                    override fun validate(input: String): Boolean = scalar.validate!!(input)
                }
            }
            else -> {
                throw IllegalArgumentException(
                        "Please specify either support object OR support functions(serialize, deserialize, validate)"
                )
            }
        }

        scalars.add(KQLObject.Scalar(scalar.name, kClass, support, scalar.description))
    }

    private fun <T : Any> ScalarDSL<T>.hasSupportFunctions(): Boolean {
        return serialize != null || deserialize != null || validate != null
    }

    inline fun <reified T : Any> scalar(noinline block: ScalarDSL<T>.() -> Unit) {
        scalar(T::class, block)
    }

    fun <T : Any>type(kClass: KClass<T>, block: TypeDSL<T>.() -> Unit){
        val type = TypeDSL(kClass, block)
        simpleTypes.add(KQLObject.Object(type.name, kClass, type.ignoredProperties.toList(), type.description))
    }

    inline fun <reified T : Any> type(noinline block: TypeDSL<T>.() -> Unit) {
        type(T::class, block)
    }

    fun <T : Enum<T>>enum(kClass: KClass<T>, enumValues : Array<T>, block: TypeDSL<T>.() -> Unit){
        val type = TypeDSL(kClass, block)
        enums.add(KQLObject.Enumeration(type.name, kClass, enumValues, type.description))
    }

    inline fun <reified T : Enum<T>> enum(noinline block: TypeDSL<T>.() -> Unit) {
        val enumValues = enumValues<T>()
        if(enumValues.isEmpty()){
            throw SchemaException("Enum of type ${T::class} must have at least one value")
        } else {
            enum(T::class, enumValues<T>(), block)
        }
    }
}

