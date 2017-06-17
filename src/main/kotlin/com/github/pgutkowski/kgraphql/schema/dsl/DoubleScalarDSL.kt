package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.scalar.DoubleScalarCoercion
import com.github.pgutkowski.kgraphql.schema.scalar.ScalarCoercion
import kotlin.reflect.KClass


class DoubleScalarDSL<T : Any>(kClass: KClass<T>, block: ScalarDSL<T, Double>.() -> Unit)
    : ScalarDSL<T, Double>(kClass, block){

    override fun createCoercionFromFunctions(): ScalarCoercion<T, Double> {
        return object : DoubleScalarCoercion<T> {

            val serializeImpl = serialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            val deserializeImpl = deserialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            override fun serialize(instance: T): Double = serializeImpl(instance)

            override fun deserialize(raw: Double): T = deserializeImpl(raw)
        }
    }

}