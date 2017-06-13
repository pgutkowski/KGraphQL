package com.github.pgutkowski.kgraphql.server

import com.github.pgutkowski.kgraphql.defaultKQLTypeName
import com.github.pgutkowski.kgraphql.schema.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.structure.SchemaNode
import kotlinx.html.*
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType


private val K_GRAPH_QL_DOCS_PREFIX = "/graphql/docs"

fun DefaultSchema.writeHomeHtml() : String {
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

fun DefaultSchema.writeQueriesHtml() : String = writeOperationsHTML("Queries", structure.queries.values)

fun DefaultSchema.writeMutationsHtml() : String = writeOperationsHTML("Mutations", structure.mutations.values)

fun DefaultSchema.writeTypeHtml(typeName: String) : String = writeTypeDescriptor(typeName)

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
    val typeByName = structure.nodes.values.find { it.kqlType.name == typeName }
    val kqlType = typeByName?.kqlType
    return when(kqlType){
        null -> throw IllegalArgumentException("Type $typeName does not exist")
        is KQLType.Object<*> -> writeObjectTypeDescriptor(typeByName, kqlType)
        is KQLType.Scalar<*> -> writeScalarTypeDescriptor(kqlType)
        is KQLType.Enumeration<*> -> writeEnumTypeDescriptor(kqlType)
        else -> throw UnsupportedOperationException("Descriptor for type $kqlType is not supported yet")
    }
}

private fun writeObjectTypeDescriptor(schemaNode: SchemaNode.Type, objectType: KQLType.Object<*>): String {
    return writeHTML {
        h1 { +objectType.name }
        hr {}

        if (objectType.description != null) {
            p { +objectType.description.toString() }
            hr {}
        }

        for ((_, property) in schemaNode.properties) {
            property(property)
        }

        for ((_, unionProperty) in schemaNode.unionProperties) {
            unionProperty(unionProperty)
        }
    }
}

private fun writeScalarTypeDescriptor(scalar: KQLType.Scalar<*>): String {
    return writeHTML {
        h1 { +scalar.name }
        hr {}

        if (scalar.description != null) {
            p { +scalar.description.toString() }
            hr {}
        }

        p { +"Scalar type declared for this schema." }
    }
}

private fun writeEnumTypeDescriptor(enum: KQLType.Enumeration<*>): String {
    return writeHTML {
        h1 { +(enum.name + " (ENUM)") }
        hr {}

        if (enum.description != null) {
            p { +enum.description.toString() }
            hr {}
        }

        for (value in enum.values) {
            p {
                +value.name
            }
        }
    }
}

private fun writeHTML(block : FlowContent.() -> Unit) : String {
    return kotlinx.html.stream.createHTML().html {
        head {
            title { +"GraphQL Docs" }
            style {
                +STYLESHEET
            }
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
        + property.kqlProperty.name
        if(property.kqlProperty is KQLProperty.Function<*>){
            arguments((property.kqlProperty as KQLProperty.Function<*>).argumentsDescriptor)
        }
        + " : "
        returnType(property.returnType)
    }
}

private fun FlowContent.unionProperty(property: SchemaNode.UnionProperty) : Unit {
    p {
        + property.kqlProperty.name
        if(property.kqlProperty is KQLProperty.Function<*>){
            arguments(property.kqlProperty.argumentsDescriptor)
        }
        + " : "
        property.returnTypes.forEachIndexed { index, returnType ->
            if(index != 0 ){
                + " | "
            }
            returnType(returnType)
        }
    }
}

private fun FlowContent.operation(operation : SchemaNode.Operation<*>) : Unit {
    p {
        + operation.kqlOperation.name
        arguments(operation.kqlOperation.argumentsDescriptor)
        + " : "
        returnType(operation.returnType)
    }
}

private fun FlowContent.returnType(type: SchemaNode.ReturnType) {
    val kqlType = type.kqlType
    when (kqlType) {
        is KQLType.Kotlin<*> -> typeLink(kqlType.kClass.starProjectedType, type.isCollection, type.isNullable)
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

private fun FlowContent.typeLink(kType: KType, collection: Boolean = false, nullable: Boolean = false) : Unit {
    if(collection) +"["
    a("$K_GRAPH_QL_DOCS_PREFIX/type/${kType.defaultKQLTypeName()}"){
        +kType.defaultKQLTypeName()
    }
    if(!nullable) +"!"
    if(collection) + "]"
}
