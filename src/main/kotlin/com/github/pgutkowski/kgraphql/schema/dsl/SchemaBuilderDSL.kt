package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.impl.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.impl.KQLObject
import com.github.pgutkowski.kgraphql.typeName
import kotlin.reflect.KClass


class SchemaBuilderDSL(private val init: SchemaBuilderDSL.() -> Unit) {

    fun build(): Schema {
        init()
        return DefaultSchema(queries, mutations, simpleTypes, scalars, enums)
    }

    private val simpleTypes = arrayListOf<KQLObject.Simple<*>>()

    private val queries = arrayListOf<KQLObject.Query<*>>()

    private val scalars = arrayListOf<KQLObject.Scalar<*>>()

    private val mutations = arrayListOf<KQLObject.Mutation<*>>()

    private val enums = arrayListOf<KQLObject.Enumeration<*>>()

    fun query(init: FunctionWrapperDSL.() -> Unit){
        val wrapperDSL = FunctionWrapperDSL(init)
        queries.add(KQLObject.Query(wrapperDSL.name, wrapperDSL.functionWrapper, wrapperDSL.description))
    }

    fun mutation(init: FunctionWrapperDSL.() -> Unit){
        val wrapperDSL = FunctionWrapperDSL(init)
        mutations.add(KQLObject.Mutation(wrapperDSL.name, wrapperDSL.functionWrapper, wrapperDSL.description))
    }

    fun <T : Any>scalar(kClass: KClass<T>, scalarSupport: ScalarSupport<T>, description: String?){
        val scalar = KQLObject.Scalar(kClass.typeName(), kClass, scalarSupport, description)
        scalars.add(scalar)
    }

    inline fun <reified T : Any> scalar(scalarSupport: ScalarSupport<T>, description : String? = null) {
        scalar(T::class, scalarSupport, description)
    }

    fun type(kClass: KClass<*>, description: String?){
        simpleTypes.add(KQLObject.Simple(kClass.typeName(), kClass, description))
    }

    inline fun <reified T : Any> type(description : String? = null) {
        type(T::class, description)
    }

    fun <T : Enum<T>>enum(kClass: KClass<T>, enumValues : Array<T>, description: String?){
        enums.add(KQLObject.Enumeration(kClass.typeName(), kClass, enumValues, description))
    }

    inline fun <reified T : Enum<T>> enum(description : String? = null) {
        val enumValues = enumValues<T>()
        if(enumValues.isEmpty()){
            throw SchemaException("Enum of type ${T::class} must have at least one value")
        } else {
            enum(T::class, enumValues<T>(), description)
        }
    }
}

