package com.github.pgutkowski.kgraphql.schema.dsl

import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.scalar.ScalarCoercion
import com.github.pgutkowski.kgraphql.schema.scalar.StringScalarCoercion
import kotlin.reflect.KClass


class StringScalarDSL<T : Any>(kClass: KClass<T>, block: ScalarDSL<T, String>.() -> Unit)
    : ScalarDSL<T, String>(kClass, block){

    override fun createCoercionFromFunctions(): ScalarCoercion<T, String> {
        return object : StringScalarCoercion<T> {

            val serializeImpl = serialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            val deserializeImpl = deserialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            override fun serialize(instance: T): String = serializeImpl(instance)

            override fun deserialize(raw: String): T = deserializeImpl(raw)
        }
    }

}