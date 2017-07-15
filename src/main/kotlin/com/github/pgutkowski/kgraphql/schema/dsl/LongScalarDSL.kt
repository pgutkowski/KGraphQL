package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.scalar.LongScalarCoercion
import com.github.pgutkowski.kgraphql.schema.scalar.ScalarCoercion
import kotlin.reflect.KClass


class LongScalarDSL<T : Any>(kClass: KClass<T>, block: ScalarDSL<T, Long>.() -> Unit)
    : ScalarDSL<T, Long>(kClass, block){

    override fun createCoercionFromFunctions(): ScalarCoercion<T, Long> {
        return object : LongScalarCoercion<T> {

            val serializeImpl = serialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            val deserializeImpl = deserialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            override fun serialize(instance: T): Long = serializeImpl(instance)

            override fun deserialize(raw: Long): T = deserializeImpl(raw)
        }
    }

}