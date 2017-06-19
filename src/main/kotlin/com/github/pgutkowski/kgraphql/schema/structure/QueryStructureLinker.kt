package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.defaultKQLTypeName
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties


class QueryStructureLinker(
        enumNodes: Map<KType, SchemaNode.Type>,
        scalarNodes: Map<KType, SchemaNode.Type>,
        val objects: List<KQLType.Object<*>>
) : AbstractStructureLinker(enumNodes, scalarNodes) {

    override fun <T : Any>handleObjectType(kClass: KClass<T>, kType: KType) : MutableSchemaNodeType {
        assertNotEnumNorFunction(kClass)

        val kqlObject = objects.find { it.kClass == kClass } ?: KQLType.Object(kType.defaultKQLTypeName(), kClass)
        val type = MutableSchemaNodeType(kqlObject)
        linkedTypes.put(kType, type)

        kClass.memberProperties
                .filterNot { kqlObject.isIgnored(it) }
                .associateTo(type.mutableProperties) { property ->
                    validateName(property.name)
                    property.name to handleKotlinProperty (
                            property, kqlObject.transformations.find { it.kProperty == property }
                    )
                }

        kqlObject.extensionProperties.associateTo(type.mutableProperties) { property ->
            property.name to handleFunctionProperty(property)
        }

        kqlObject.unionProperties.associateTo(type.mutableUnionProperties) { property ->
            property.name to handleUnionProperty(property)
        }

        if(type.mutableProperties.isEmpty() && type.mutableUnionProperties.isEmpty()){
            throw SchemaException("An Object type must define one or more fields. Found none on type ${kqlObject.name}")
        }

        return type
    }
}