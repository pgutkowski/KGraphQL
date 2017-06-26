package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.defaultKQLTypeName
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties


class InputStructureLinker(
        enumNodes: Map<KType, SchemaNode.Type>,
        scalarNodes: Map<KType, SchemaNode.Type>,
        val inputs: List<KQLType.Input<*>>
) : AbstractStructureLinker(enumNodes, scalarNodes) {

    override fun <T : Any>handleObjectType(kClass: KClass<T>, kType: KType) : MutableSchemaNodeType {
        assertValidObjectType(kType)

        val kqlObject = inputs.find { it.kClass == kClass } ?: KQLType.Input(kType.defaultKQLTypeName(), kClass, null)
        val type = MutableSchemaNodeType(kqlObject)
        linkedTypes.put(kType, type)

        kClass.memberProperties
                .associateTo(type.mutableProperties) { property ->
                    validateName(property.name)
                    property.name to handleKotlinProperty (property, null)
                }

        validateSchemaNodeType(type)

        return type
    }
}