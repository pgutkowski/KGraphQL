package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.ValidationException
import com.github.pgutkowski.kgraphql.request.graph.Fragment
import com.github.pgutkowski.kgraphql.request.graph.SelectionNode
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLType

fun validatePropertyArguments(property: SchemaNode.Property, kqlType: KQLType, requestNode: SelectionNode) {

    val kqlProperty = property.kqlProperty

    fun illegalArguments(): List<ValidationException> {
        return listOf(ValidationException(
                "Property ${kqlProperty.name} on type ${kqlType.name} has no arguments, found: ${requestNode.arguments?.map { it.key }}")
        )
    }

    val argumentValidationExceptions = when {
    //extension property function
        kqlProperty is KQLProperty.Function<*> -> {
            kqlProperty.validateArguments(requestNode.arguments)
        }
    //property with transformation
        kqlProperty is KQLProperty.Kotlin<*, *> && kqlType is KQLType.Object<*> -> {
            property.transformation
                    ?.validateArguments(requestNode.arguments)
                    ?: if(requestNode.arguments == null) emptyList() else illegalArguments()
        }
        requestNode.arguments == null -> emptyList()
        else -> illegalArguments()
    }

    if (argumentValidationExceptions.isNotEmpty()) {
        throw ValidationException(argumentValidationExceptions.fold("", { sum, exc -> sum + "${exc.message}; " }))
    }
}

/**
 * validate that only typed fragments are present
 */
fun validateUnionRequest(requestNode: SelectionNode, property: SchemaNode.UnionProperty) {
    val illegalChildren = requestNode.children?.filterNot {
        it is Fragment.Inline || it is Fragment.External
    }

    if (illegalChildren?.any() ?: false) {
        throw RequestException(
                "Invalid selection set with properties: $illegalChildren " +
                        "on union type property ${property.kqlProperty.name} : ${property.returnTypes.map { it.kqlType.name }}"
        )
    }
}

fun validateName(name : String) {
    if(name.startsWith("__")){
        throw SchemaException("Illegal name '$name'. " +
                "Names starting with '__' are reserved for introspection system"
        )
    }
}
