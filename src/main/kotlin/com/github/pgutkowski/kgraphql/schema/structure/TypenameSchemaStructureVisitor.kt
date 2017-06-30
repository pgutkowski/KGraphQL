package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.schema.model.FunctionWrapper
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import kotlin.reflect.full.starProjectedType


class TypenameSchemaStructureVisitor(val typeDefinitionProvider: TypeDefinitionProvider) {

    private fun buildResolver(baseKQLType: KQLType) : (Any) -> String {
        return { value : Any ->
            typeDefinitionProvider.typeByKClass(value.javaClass.kotlin)?.name ?: baseKQLType.name
        }
    }

    fun visit(structure: SchemaStructure){
        val returnType = structure.queryTypes[String::class.starProjectedType]
                ?: throw IllegalStateException("Cannot add __typename property to ")

        structure.queryTypes.values
                .filterIsInstance(MutableSchemaNodeType::class.java)
                .forEach { addTypeNameProperty(it, returnType) }
    }

    private fun addTypeNameProperty(type: MutableSchemaNodeType, stringType: SchemaNode.Type) {
        val functionWrapper = FunctionWrapper.on (buildResolver(type.kqlType), true)
        val __typenameKQLProperty = KQLProperty.Function("__typename", functionWrapper)
        val propertyReturnType = SchemaNode.ReturnType(stringType)
        type.mutableProperties.put("__typename", SchemaNode.Property(__typenameKQLProperty, propertyReturnType))
    }

}