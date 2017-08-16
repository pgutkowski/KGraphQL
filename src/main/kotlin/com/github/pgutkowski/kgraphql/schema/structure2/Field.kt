package com.github.pgutkowski.kgraphql.schema.structure2

import com.github.pgutkowski.kgraphql.schema.introspection.NotIntrospected
import com.github.pgutkowski.kgraphql.schema.introspection.__Field
import com.github.pgutkowski.kgraphql.schema.introspection.__InputValue
import com.github.pgutkowski.kgraphql.schema.introspection.__Type
import com.github.pgutkowski.kgraphql.schema.model.BaseOperationDef
import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.PropertyDef
import com.github.pgutkowski.kgraphql.schema.model.Transformation
import kotlin.reflect.full.findAnnotation


sealed class Field : __Field {

    abstract val arguments : List<InputValue<*>>

    override val args: List<__InputValue>
        get() = arguments.filterNot { it.type.kClass?.findAnnotation<NotIntrospected>() != null }

    abstract val returnType : Type

    override val type: __Type
        get() = returnType

    class Function<T>(
            kql : BaseOperationDef<T>,
            override val returnType: Type,
            override val arguments: List<InputValue<*>>
    ) : Field(), FunctionWrapper<T> by kql {

        override val name: String = kql.name

        override val description: String? = kql.description

        override val isDeprecated: Boolean = kql.isDeprecated

        override val deprecationReason: String? = kql.deprecationReason
    }

    class Kotlin<T : Any, R>(
            kql : PropertyDef.Kotlin<T, R>,
            override val returnType: Type,
            override val arguments: List<InputValue<*>>,
            val transformation : Transformation<T, R>? = null
    ) : Field(){

        val kProperty = kql.kProperty

        override val name: String = kql.name

        override val description: String? = kql.description

        override val isDeprecated: Boolean = kql.isDeprecated

        override val deprecationReason: String? = kql.deprecationReason
    }

    class Union(
            kql : PropertyDef.Union,
            override val returnType: Type.Union,
            override val arguments: List<InputValue<*>>
    ) : Field(), FunctionWrapper<Any?> by kql {

        override val name: String = kql.name

        override val description: String? = kql.description

        override val isDeprecated: Boolean = kql.isDeprecated

        override val deprecationReason: String? = kql.deprecationReason
    }
}