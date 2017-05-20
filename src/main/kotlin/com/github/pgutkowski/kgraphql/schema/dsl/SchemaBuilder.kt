package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.ScalarSupport
import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.builtin.BuiltInType
import com.github.pgutkowski.kgraphql.schema.impl.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.model.*
import com.github.pgutkowski.kgraphql.typeName
import kotlin.reflect.KClass


class SchemaBuilder(private val init: SchemaBuilder.() -> Unit) {

    fun build(): Schema {
        init()
        return DefaultSchema(queries, mutations, objects, scalars, enums, unions)
    }

    internal val objects = arrayListOf<KQLType.Object<*>>()

    internal val queries = arrayListOf<KQLQuery<*>>()

    internal val scalars = arrayListOf<KQLType.Scalar<*>>(
            BuiltInType.STRING,
            BuiltInType.BOOLEAN,
            BuiltInType.DOUBLE,
            BuiltInType.FLOAT,
            BuiltInType.INT
    )

    internal val mutations = arrayListOf<KQLMutation<*>>()

    internal val enums = arrayListOf<KQLType.Enumeration<*>>()

    internal val unions = arrayListOf<KQLType.Union>()

    fun query(name : String, init: QueryOrMutationDSL.() -> Unit){
        val wrapperDSL = QueryOrMutationDSL(init)
        wrapperDSL.name = name
        queries.add(KQLQuery(wrapperDSL.name, wrapperDSL.functionWrapper, wrapperDSL.description))
    }

    fun query(init: QueryOrMutationDSL.() -> Unit){
        val wrapperDSL = QueryOrMutationDSL(init)
        queries.add(KQLQuery(wrapperDSL.name, wrapperDSL.functionWrapper, wrapperDSL.description))
    }

    fun mutation(init: QueryOrMutationDSL.() -> Unit){
        val wrapperDSL = QueryOrMutationDSL(init)
        mutations.add(KQLMutation(wrapperDSL.name, wrapperDSL.functionWrapper, wrapperDSL.description))
    }

    fun <T : Any>supportedScalar(kClass: KClass<T>, block: SupportedScalarDSL<T>.() -> Unit){
        val scalar = SupportedScalarDSL(kClass, block)
        val support = scalar.support ?: throw SchemaException("Please specify scalar support object")

        scalars.add(KQLType.Scalar(scalar.name, kClass, support, scalar.description))
    }

    inline fun <reified T : Any> supportedScalar(noinline block: SupportedScalarDSL<T>.() -> Unit) {
        supportedScalar(T::class, block)
    }

    fun <T : Any>scalar(kClass: KClass<T>, block: ScalarDSL<T>.() -> Unit){
        val scalar = ScalarDSL(kClass, block)

        if(scalar.missingSupportFunction()) throw SchemaException("Please specify scalar support functions")

        val support : ScalarSupport<T> = object : ScalarSupport<T> {
            override fun serialize(input: String): T = scalar.serialize!!(input)
            override fun deserialize(input: T): String = scalar.deserialize!!(input)
            override fun validate(input: String): Boolean = scalar.validate!!(input)
        }

        scalars.add(KQLType.Scalar(scalar.name, kClass, support, scalar.description))
    }

    private fun <T : Any> ScalarDSL<T>.missingSupportFunction(): Boolean {
        return serialize == null || deserialize == null || validate == null
    }

    inline fun <reified T : Any> scalar(noinline block: ScalarDSL<T>.() -> Unit) {
        scalar(T::class, block)
    }

    fun <T : Any>type(kClass: KClass<T>, block: TypeDSL<T>.() -> Unit){
        val type = TypeDSL(this.unions, kClass, block)
        objects.add(type.toKQLObject())
    }

    inline fun <reified T : Any> type(noinline block: TypeDSL<T>.() -> Unit) {
        type(T::class, block)
    }

    fun <T : Enum<T>>enum(kClass: KClass<T>, enumValues : Array<T>, block: EnumDSL<T>.() -> Unit){
        val type = EnumDSL(kClass, block)
        enums.add(KQLType.Enumeration(type.name, kClass, enumValues, type.description))
    }

    inline fun <reified T : Enum<T>> enum(noinline block: EnumDSL<T>.() -> Unit) {
        val enumValues = enumValues<T>()
        if(enumValues.isEmpty()){
            throw SchemaException("Enum of type ${T::class} must have at least one value")
        } else {
            enum(T::class, enumValues<T>(), block)
        }
    }

    fun unionType(block : UnionTypeDSL.() -> Unit){
        val union = UnionTypeDSL(block)
        val possibleTypes = union.possibleTypes.map { kClass ->
            var registeredObject = objects.find { it.kClass == kClass }
            if(registeredObject == null){
                registeredObject = KQLType.Object(kClass.typeName(), kClass)
                objects.add(registeredObject)
            }
            registeredObject!!
        }
        unions.add(KQLType.Union(union.name, possibleTypes, union.description))
    }
}

