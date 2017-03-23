package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.annotation.type.*
import com.github.pgutkowski.kql.schema.Schema
import com.github.pgutkowski.kql.schema.SchemaBuilder
import com.github.pgutkowski.kql.schema.SchemaException
import com.github.pgutkowski.kql.support.ClassSupport
import com.github.pgutkowski.kql.support.ScalarSupport
import javax.naming.OperationNotSupportedException
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation


open class DefaultSchemaBuilder : SchemaBuilder {

    private val queries = arrayListOf<KQLType.Query<*>>()
    private val inputs = arrayListOf<KQLType.Input<*>>()
    private val mutations = arrayListOf<KQLType.Mutation<*>>()
    private val scalars = arrayListOf<KQLType.Scalar<*>>()
    private val simple = arrayListOf<KQLType.Simple<*>>()
    private val interfaces = arrayListOf<KQLType.Interface<*>>()

    override fun <T : Any> addClass(kClass: KClass<T>) : SchemaBuilder {
        return addSupportedClass(kClass)
    }

    override fun <T : Any> addSupportedClass(kClass: KClass<T>, vararg classSupport: ClassSupport<T>): SchemaBuilder {
        addIfQuery(kClass, classSupport)
        addIfMutation(kClass, classSupport)
        addIfInput(kClass, classSupport)
        addIfScalar(kClass, classSupport)
        addIfSimple(kClass, classSupport)
        addIfInterface(kClass, classSupport)
        return this
    }

    override fun build(): Schema {

        return DefaultSchema (
                queries = queries,
                inputs = inputs,
                mutations = mutations,
                scalars = scalars,
                simple = simple,
                interfaces = interfaces
        )
    }

    /**
     * right now it is dirty workaround to see if there is any Scalar support for class annotated as scalar
     */
    fun <T : Any> validateScalar(kClass: KClass<T>, classSupport: Array<out ClassSupport<*>>) : Boolean {

        fun isScalarSupport(it: ClassSupport<*>): Boolean {
            return it is ScalarSupport<*, *> && it.javaClass.getMethod("deserialize", kClass.java) != null
        }

        return classSupport.any(::isScalarSupport)
    }

    fun <T : Any>addIfQuery(kClass: KClass<T>, classSupport: Array<out ClassSupport<T>>){
        if(kClass.isQuery()) queries.add(KQLType.Query(kClass.simpleName!!, kClass, classSupport.toList()))
    }

    fun <T : Any>addIfMutation(kClass: KClass<T>, classSupport: Array<out ClassSupport<T>>){
        if(kClass.isMutation()) mutations.add(KQLType.Mutation(kClass.simpleName!!, kClass, classSupport.toList()))
    }

    fun <T : Any>addIfInput(kClass: KClass<T>, classSupport: Array<out ClassSupport<T>>){
        if(kClass.isInput()) inputs.add(KQLType.Input(kClass.simpleName!!, kClass, classSupport.toList()))
    }

    fun <T : Any>addIfScalar(kClass: KClass<T>, classSupport: Array<out ClassSupport<T>>){
        if(!validateScalar(kClass, classSupport)){
            throw SchemaException("Invalid scalar class: $kClass. Please register instance of ScalarSupport for this class")
        }
        if(kClass.isScalar()) scalars.add(KQLType.Scalar(kClass.simpleName!!, kClass, classSupport.toList()))
    }

    fun <T : Any>addIfSimple(kClass: KClass<T>, classSupport: Array<out ClassSupport<T>>){
        if(kClass.isSimple()) simple.add(KQLType.Simple(kClass.simpleName!!, kClass, classSupport.toList()))
    }

    fun <T : Any>addIfInterface(kClass: KClass<T>, classSupport: Array<out ClassSupport<T>>){
        throw OperationNotSupportedException("Interfaces are not supported yet")
//        if(kClass.isInterface()) interfaces.add(KQLType.Interface(kClass.simpleName!!, kClass))
    }

    fun <T : Any> KClass<T>.isInterface() = java.isInterface

    fun <T : Any> KClass<T>.isQuery() = findAnnotation<Query>() != null

    fun <T : Any> KClass<T>.isMutation() = findAnnotation<Mutation>() != null

    fun <T : Any> KClass<T>.isInput() = findAnnotation<Input>() != null

    fun <T : Any> KClass<T>.isScalar() = findAnnotation<Scalar>() != null

    fun <T : Any> KClass<T>.isSimple(): Boolean {
        return findAnnotation<Type>() != null || !(isQuery() || isMutation() || isInput() || isScalar() )
    }
}
