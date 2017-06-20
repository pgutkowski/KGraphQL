package com.github.pgutkowski.kgraphql.schema.structure

import com.github.pgutkowski.kgraphql.isCollection
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.model.BaseKQLOperation
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.Transformation
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure


abstract class AbstractStructureLinker (
        val enumNodes: Map<KType, SchemaNode.Type>,
        val scalarNodes: Map<KType, SchemaNode.Type>
) {

    val linkedTypes = mutableMapOf<KType, SchemaNode.Type>()

    val foundInputTypes = mutableSetOf<KType>()

    abstract fun <T : Any>handleObjectType(kClass: KClass<T>, kType: KType) : MutableSchemaNodeType

    fun <R> handleOperation(operation : BaseKQLOperation<R>) : SchemaNode.ReturnType {
        foundInputTypes += operation.argumentsDescriptor.values
        return linkType(operation.kFunction.returnType)
    }

    fun linkType(kType: KType) : SchemaNode.ReturnType {
        val kClass = kType.jvmErasure
        if(kClass.isCollection()){
            val elementType = kType.arguments.getOrNull(0)?.type
                    ?: throw SchemaException("Cannot infer collection element type for type $kType")
            return handleCollectionType(kType, elementType)
        } else {
            val isNullable: Boolean = kType.isMarkedNullable
            val type = getType(kClass, kType)
            return SchemaNode.ReturnType(type, false, isNullable, false)
        }
    }

    fun getType(kClass: KClass<*>, kType: KType): SchemaNode.Type {
        val lookupType = kType.withNullability(false)

        return linkedTypes[lookupType]
                ?: enumNodes[lookupType]
                ?: scalarNodes[lookupType]
                ?: handleObjectType(kClass, kType)
    }

    protected fun handleCollectionType(collectionKType: KType, entryKType: KType) : SchemaNode.ReturnType {
        val isNullable: Boolean = collectionKType.isMarkedNullable
        val areEntriesNullable : Boolean = entryKType.isMarkedNullable

        val entryKClass = entryKType.jvmErasure
        if(entryKClass.isCollection()){
            throw IllegalArgumentException("Nested Collections are not supported")
        } else {
            val type = getType(entryKClass, entryKType)
            return SchemaNode.ReturnType(type, true, isNullable, areEntriesNullable)
        }
    }

    protected fun handleFunctionProperty(property: KQLProperty.Function<*>): SchemaNode.Property {
        if(property is KQLProperty.Union){
            throw IllegalArgumentException("Cannot handle union function property")
        } else {
            foundInputTypes += property.argumentsDescriptor.values
            return SchemaNode.Property(property, linkType(property.kFunction.returnType))
        }
    }

    protected fun handleUnionProperty(property: KQLProperty.Union) : SchemaNode.UnionProperty {
        return SchemaNode.UnionProperty(property, property.union.members.map { linkType(it.starProjectedType) })
    }

    protected fun <T> handleKotlinProperty(property: KProperty1<T, *>, transformation: Transformation<out Any, out Any?>?) : SchemaNode.Property {
        return SchemaNode.Property(KQLProperty.Kotlin(property), linkType(property.returnType), transformation)
    }

    fun <T : Any> assertNotEnumNorFunction(kClass: KClass<T>) {
        when {
            kClass.isSubclassOf(Enum::class) ->
                throw SchemaException("Cannot handle enum class $kClass as Object type")
            kClass.isSubclassOf(Function::class) ->
                throw SchemaException("Cannot handle function $kClass as Object type")

        }
    }
}