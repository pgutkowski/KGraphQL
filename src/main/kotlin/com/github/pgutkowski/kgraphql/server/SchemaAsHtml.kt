package com.github.pgutkowski.kgraphql.server

import com.github.pgutkowski.kgraphql.schema.impl.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.impl.SchemaNode
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.typeName
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType


private val K_GRAPH_QL_DOCS_PREFIX = "/graphql/docs"

fun DefaultSchema.asHTML(path : List<String>) : String {

    if (path.isEmpty()) {
        return homeHTML()
    }

    return when (path[0]) {
        "query" -> writeOperationsHTML("Queries", structure.queries.values)
        "mutation" -> writeOperationsHTML("Mutations", structure.mutations.values)
        "type" -> when (path[1].toLowerCase()) {
            else -> writeTypeDescriptor(path[1])
        }
        else -> throw IllegalArgumentException("Illegal request")
    }
}

private fun writeOperationsHTML(title: String, operations: Collection<SchemaNode.Operation<*>>): String {
    return writeHTML {
        h1 { +title }
        hr {}
        for (operation in operations) {
            operation(operation)
        }
    }
}

private fun DefaultSchema.writeTypeDescriptor(typeName : String) : String {
    val typeByName = structure.nodes.values.find { it.name == typeName }
    val kqlType = typeByName?.kqlType
    return when(kqlType){
        null -> throw IllegalArgumentException("Type $typeName does not exist")
        is KQLType.Object<*> -> writeObjectTypeDescriptor(typeByName, kqlType)
        is KQLType.Scalar<*> -> writeScalarTypeDescriptor(kqlType)
        is KQLType.Enumeration<*> -> writeEnumTypeDescriptor(kqlType)
        else -> throw UnsupportedOperationException("Descriptor for type $kqlType is not supported yet")
    }
}

fun writeObjectTypeDescriptor(schemaNode: SchemaNode.Type, objectType: KQLType.Object<*>): String {
    return writeHTML {
        h1 { +objectType.name }
        hr {}

        if(objectType.description != null){
            p {+ objectType.description }
            hr {}
        }

        for ((_, property) in schemaNode.properties){
            property(property)
        }
    }
}

fun writeScalarTypeDescriptor(scalar: KQLType.Scalar<*>): String {
    return writeHTML {
        h1 { +scalar.name }
        hr {}

        if(scalar.description != null){
            p {+ scalar.description }
            hr {}
        }

        p { +"Scalar type declared for this schema." }
    }
}

private fun writeEnumTypeDescriptor(enum: KQLType.Enumeration<*>): String {
    return writeHTML {
        h1 { +(enum.name + " (ENUM)") }
        hr {}

        if(enum.description != null){
            p {+enum.description}
            hr {}
        }

        for (value in enum.values) {
            p {
                +value.name
            }
        }
    }
}

private fun homeHTML(): String {
    return writeHTML {
        p {
            a("$K_GRAPH_QL_DOCS_PREFIX/query") {
                +"Queries"
            }
        }
        p {
            a("$K_GRAPH_QL_DOCS_PREFIX/mutation") {
                +"Mutations"
            }
        }
    }
}

private fun writeHTML(block : FlowContent.() -> Unit) : String {
    return createHTML().html {
        head {
            title { +"GraphQL Docs" }
        }
        body {
            h4 { +"GraphQL Docs" }
            hr{}
            block()
        }
    }
}

private fun FlowContent.property(property: SchemaNode.Property) : Unit {
    p {
        + property.name
        if(property.kqlProperty is KQLProperty.Function<*>){
            arguments(property.kqlProperty.argumentsDescriptor)
        }
        + " : "
        returnType(property.returnType)
    }
}

private fun FlowContent.operation(operation : SchemaNode.Operation<*>) : Unit {
    p {
        + operation.name
        arguments(operation.kqlOperation.argumentsDescriptor)
        + " : "
        returnType(operation.returnType)
    }
}

private fun FlowContent.returnType(type: SchemaNode.ReturnType) {
    val kqlType = type.kqlType
    when (kqlType) {
        is KQLType.Kotlin<*> -> typeLink(kqlType.kClass.starProjectedType, type.isCollection)
        else -> throw UnsupportedOperationException("Type representation for $kqlType is not supported yet")
    }
}

private fun FlowContent.arguments(args: Map<String, KType>?) : Unit {
    if(args != null && args.isNotEmpty()){
        +"("
        args.toList().forEachIndexed { index, (name, argType) ->
            if(index > 0 ) +(", ")
            +(name + " : ")
            typeLink(argType)
        }
        +")"
    }
}

private fun FlowContent.typeLink(kType: KType, collection: Boolean = false) : Unit {
    a("$K_GRAPH_QL_DOCS_PREFIX/type/${kType.typeName()}"){
        if(collection) +"["
        +kType.nullableTypeName()
        if(collection) + "]"
    }
}

private fun KType.nullableTypeName() : String {
    return typeName() + if(isMarkedNullable) "" else "!"
}
