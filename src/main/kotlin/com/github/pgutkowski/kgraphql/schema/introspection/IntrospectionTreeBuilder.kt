package com.github.pgutkowski.kgraphql.schema.introspection

import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.structure.SchemaNode


class IntrospectionTreeBuilder {

    fun introspectType(type:SchemaNode.Type) : __Type {

        val kind: __TypeKind = determineKind(type)

        val fields : List<__Field>? = introspectFields(type)

        val enumValues : List<__EnumValue>? = introspectEnumValues(type)

        val inputFields : List<__InputValue>? = introspectInputValues(type)

        //TODO: persist information about inheritance tree in schemas
        return __Type(
                kind = kind,
                name = type.kqlType.name,
                description = type.kqlType.description ?: "",
                fields = fields,
                interfaces = emptyList(),
                possibleTypes = emptyList(),
                enumValues = enumValues,
                inputFields = inputFields
        )
    }

    fun determineKind(type: SchemaNode.Type) = when(type.kqlType) {
        is KQLType.Scalar<*> -> __TypeKind.SCALAR
        is KQLType.Enumeration<*> -> __TypeKind.ENUM
        is KQLType.Object<*> -> __TypeKind.OBJECT
        is KQLType.Interface<*> -> __TypeKind.INTERFACE
        is KQLType.Input<*> -> __TypeKind.INPUT_OBJECT
        is KQLType.Union -> __TypeKind.UNION
        else -> throw IllegalStateException("Unexpected KQLType: ${type.kqlType}")
    }

    fun introspectFields(type: SchemaNode.Type) = when(type.kqlType){
        is KQLType.Object<*>, is KQLType.Interface<*> -> {
            type.properties.map { introspectField(it.value) } + type.unionProperties.map { introspectUnionField(it.value) }
        }
        else -> null
    }

    fun introspectField(field : SchemaNode.Property) = __Field (
            field.kqlProperty.name,
            field.kqlProperty.description,
            introspectType(field.returnType),
            emptyList(),
            field.kqlProperty.isDeprecated,
            field.kqlProperty.deprecationReason
    )

    fun introspectUnionField(field : SchemaNode.UnionProperty) = __Field (
            field.kqlProperty.name,
            field.kqlProperty.description,
            introspectUnionType(field.kqlProperty.union),
            emptyList(),
            field.kqlProperty.isDeprecated,
            field.kqlProperty.deprecationReason
    )

    private fun introspectUnionType(union: KQLType.Union) = __Type(
            kind = __TypeKind.UNION,
            name = union.name,
            description = union.description ?: "",
            fields = emptyList(),
            interfaces = emptyList(),
            possibleTypes = emptyList(),
            enumValues = emptyList(),
            inputFields = emptyList()
    )

    fun introspectEnumValues(type: SchemaNode.Type) = when(type.kqlType){
        is KQLType.Enumeration<*> -> type.kqlType.values.map {
            __EnumValue(it.name, it.description, it.isDeprecated, it.deprecationReason)
        }
        else -> null
    }

    fun introspectInputValues(type: SchemaNode.Type) = when(type.kqlType){
        is KQLType.Input<*> -> type.properties.map {
            __InputValue(introspectType(it.value.returnType), null, it.value.kqlProperty.name, null)
        }
        else -> null
    }

}