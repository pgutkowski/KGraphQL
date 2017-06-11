package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.SyntaxException
import com.github.pgutkowski.kgraphql.ValidationException
import com.github.pgutkowski.kgraphql.graph.Fragment
import com.github.pgutkowski.kgraphql.graph.GraphNode
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLType

fun validatePropertyArguments(property: SchemaNode.Property, kqlType: KQLType, requestNode: GraphNode) {

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
fun validateUnionRequest(requestNode: GraphNode, property: SchemaNode.UnionProperty) {
    val illegalChildren = requestNode.children?.filterNot {
        it is Fragment.Inline || it is Fragment.External
    }

    if (illegalChildren?.any() ?: false) {
        throw SyntaxException(
                "Invalid selection set with properties: $illegalChildren " +
                        "on union type property ${property.kqlProperty.name} : ${property.returnTypes.map { it.kqlType.name }}"
        )
    }
}

