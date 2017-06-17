package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.scalar.IntScalarCoercion
import com.github.pgutkowski.kgraphql.schema.scalar.ScalarCoercion
import kotlin.reflect.KClass


class IntScalarDSL<T : Any>(kClass: KClass<T>, block: ScalarDSL<T, Int>.() -> Unit)
    : ScalarDSL<T, Int>(kClass, block){

    override fun createCoercionFromFunctions(): ScalarCoercion<T, Int> {
        return object : IntScalarCoercion<T> {

            val serializeImpl = serialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            val deserializeImpl = deserialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            override fun serialize(instance: T): Int = serializeImpl(instance)

            override fun deserialize(raw: Int): T = deserializeImpl(raw)
        }
    }

}