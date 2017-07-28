package com.github.pgutkowski.kgraphql.schema.structure2

import com.github.pgutkowski.kgraphql.RequestException
import com.github.pgutkowski.kgraphql.ValidationException
import com.github.pgutkowski.kgraphql.request.Arguments
import com.github.pgutkowski.kgraphql.request.graph.Fragment
import com.github.pgutkowski.kgraphql.request.graph.SelectionNode
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.introspection.TypeKind
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun validatePropertyArguments(parentType: Type, field: Field, requestNode: SelectionNode) {

    val argumentValidationExceptions = field.validateArguments(requestNode.arguments, parentType.name)

    if (argumentValidationExceptions.isNotEmpty()) {
        throw ValidationException(argumentValidationExceptions.fold("", { sum, exc -> sum + "${exc.message}; " }))
    }
}

fun Field.validateArguments(selectionArgs: Arguments?, parentTypeName : String?) : List<ValidationException> {
    if(this.arguments.isEmpty() && selectionArgs?.isNotEmpty() ?: false){
        return listOf(ValidationException(
                "Property $name on type $parentTypeName has no arguments, found: ${selectionArgs?.map { it.key }}")
        )
    }

    val exceptions = mutableListOf<ValidationException>()

    val parameterNames = arguments.map { it.name }
    val invalidArguments = selectionArgs?.filterKeys { it !in parameterNames }

    if(invalidArguments != null && invalidArguments.isNotEmpty()){
        exceptions.add(ValidationException("${this.name} does support arguments ${arguments.map { it.name }}. " +
                "Found arguments ${selectionArgs.map { it.key }}"))
    }

    arguments.forEach { arg ->
        val value = selectionArgs?.get(arg.name)
        if(value != null || arg.type.kind != TypeKind.NON_NULL || arg.defaultValue != null){
            //is valid
        } else {
            exceptions.add(ValidationException("Missing value for non-nullable argument ${arg.name} on field '${this.name}'"))
        }
    }
    return exceptions
}


/**
 * validate that only typed fragments are present
 */
fun validateUnionRequest(field: Field.Union, selectionNode: SelectionNode) {
    val illegalChildren = selectionNode.children?.filterNot {
        it is Fragment.Inline || it is Fragment.External
    }

    if (illegalChildren?.any() ?: false) {
        throw RequestException(
                "Invalid selection set with properties: $illegalChildren " +
                        "on union type property ${field.name} : ${field.returnType.possibleTypes.map { it.name }}"
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

fun assertValidObjectType(kClass: KClass<*>) {
    when {
    //function before generic, because it is its subset
        kClass.isSubclassOf(Function::class) ->
            throw SchemaException("Cannot handle function $kClass as Object type")
        kClass.isSubclassOf(Enum::class) ->
            throw SchemaException("Cannot handle enum class $kClass as Object type")
    }
}