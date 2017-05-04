package com.github.pgutkowski.kgraphql.server

import com.github.pgutkowski.kgraphql.graph.DescriptorNode
import com.github.pgutkowski.kgraphql.graph.Graph
import com.github.pgutkowski.kgraphql.schema.impl.KQLObject
import com.github.pgutkowski.kgraphql.schema.impl.SchemaDescriptor
import com.github.pgutkowski.kgraphql.typeName
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType


fun SchemaDescriptor.asHTML(path : List<String>) : String {

    if (path.isEmpty()) {
        return homeHTML()
    }

    return when (path[0]) {
        "query" -> writeHTMLGraph("Queries", queries)
        "mutation" -> writeHTMLGraph("Mutations", mutations)
        "type" -> when (path[1].toLowerCase()) {
            "string", "int", "boolean", "double" -> writeBuiltInTypeDescriptor(path[1])
            else -> writeTypeDescriptor(path[1])
        }
        else -> throw IllegalArgumentException("Illegal request")
    }
}

private fun List<KQLObject>.findByName(name : String): KQLObject? {
    return find { it.name.equals(name, true)  }
}

private fun SchemaDescriptor.writeTypeDescriptor(typeName : String) : String {
    val typeByName = findDescribedTypeByName(typeName, typeMap)
    if(typeByName != null){
        return writeHTMLGraph(typeName, typeByName, schema.types.findByName(typeName)?.description)
    } else {
        val enumByName = findDescribedEnumByName(typeName, schema.enums)
        if(enumByName != null){
            return writeEnumTypeDescriptor(enumByName)
        } else {
            val scalarByName = findDescribedScalarByName(typeName, schema.scalars) ?: throw IllegalArgumentException("Type: $typeName not found")
            return writeScalarTypeDescriptor(scalarByName)
        }
    }
}

fun writeScalarTypeDescriptor(scalarByName: KQLObject.Scalar<*>): String {
    return writeHTML {
        h1 { +scalarByName.name }
        hr {}

        if(scalarByName.description != null){
            p {+ scalarByName.description }
            hr {}
        }

        p { +"Scalar type declared for this schema." }
    }
}

private fun writeBuiltInTypeDescriptor(name : String) : String {
    return writeHTML {
        h1 { +name }
        hr {}
        p { +"Built-in scalar type." }
    }
}

private fun writeEnumTypeDescriptor(enumByName: KQLObject.Enumeration<*>): String {
    return writeHTML {
        h1 { +(enumByName.name + " (ENUM)") }
        hr {}

        if(enumByName.description != null){
            p {+enumByName.description}
            hr {}
        }

        for (value in enumByName.values) {
            p {
                +value.name
            }
        }
    }
}

private fun writeHTMLGraph(title: String, root: Graph, description: String? = null): String {
    return writeHTML {
        h1 { +title }
        hr {}

        if(description != null){
            p {+description}
            hr {}
        }

        for (node in root) {
            field(node as DescriptorNode)
        }
    }
}

private fun findDescribedScalarByName(typeName: String, scalars: List<KQLObject.Scalar<*>>): KQLObject.Scalar<*>? {
    return scalars.firstOrNull{it.name.equals(typeName, true)}
}

private fun findDescribedEnumByName(name: String, enums: List<KQLObject.Enumeration<*>>) : KQLObject.Enumeration<*>? {
    return enums.firstOrNull { it.name.equals(name, true)}
}

private fun findDescribedTypeByName(name : String, typeMap: Map<KClass<*>, Graph>) : Graph? {
    for((kClass, graph) in typeMap){
        if(kClass.starProjectedType.typeName() == name) return graph
    }
    return null
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

private fun FlowContent.field(node : DescriptorNode) : Unit {
    p {
        + node.key
        arguments(node.argumentsDescriptor)
        + " : "
        typeLink(node.type, node.isCollection)
    }
}

private val K_GRAPH_QL_DOCS_PREFIX = "/graphql/docs"

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
