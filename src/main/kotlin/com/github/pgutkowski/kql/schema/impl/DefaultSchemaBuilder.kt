package com.github.pgutkowski.kql.schema.impl

import com.github.pgutkowski.kql.schema.Schema
import com.github.pgutkowski.kql.schema.SchemaBuilder
import com.github.pgutkowski.kql.support.ClassSupport
import kotlin.reflect.KClass


open class DefaultSchemaBuilder : SchemaBuilder {

    private val classes = hashMapOf<KClass<*>, Array<out ClassSupport<*>>>()

    override fun addClass(clazz: KClass<*>) : SchemaBuilder {
        this.classes.put(clazz, emptyArray())
        return this
    }

    override fun <T : Any> addSupportedClass(kClass: KClass<T>, vararg classSupport: ClassSupport<T>): SchemaBuilder {
        classes.put(kClass, classSupport)
        return addClass(kClass)
    }

    override fun build(): Schema {
        return DefaultSchema(classes)
    }
}
