package com.github.pgutkowski.kgraphql.schema.introspection

import com.github.pgutkowski.kgraphql.isCollection
import com.github.pgutkowski.kgraphql.schema.DefaultSchema
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.directive.Directive
import com.github.pgutkowski.kgraphql.schema.model.KQLOperation
import com.github.pgutkowski.kgraphql.schema.model.KQLProperty
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.schema.structure.SchemaNode
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure


class SchemaIntrospection(val schema: DefaultSchema) : __Schema {

    val typeMap = mutableMapOf<String, __Type>()

    override val types: List<__Type>
        get() = typeMap.values.toList()

    init {
        introspectTypes(schema.structure.queryTypes.values)
        introspectTypes(schema.structure.inputTypes.values)
    }

    override val queryType: __Type = introspectQueryType(schema.structure.queries)

    override val mutationType: __Type? = introspectMutationType(schema.structure.mutations)

    override val subscriptionType: __Type? = null

    override val directives: List<__Directive> = introspectDirectives(schema.definition.directives)

    override fun findTypeByName(name: String): __Type? = typeMap[name]

    private fun introspectTypes(types: Collection<SchemaNode.Type>){
        types.forEach {
            typeMap.getOrPut(it.kqlType.name){ introspectType(it) }
        }
    }

    private fun introspectDirectives(directives: List<Directive>): List<__Directive>{
        return directives.map { __Directive(it.name, null, it.locations.toList(), emptyList()) }
    }

    private fun introspectMutationType(mutations: Map<String, SchemaNode.Mutation<*>>): __Type? {
        if(mutations.isEmpty()) return null

        val fields = mutations.values.map { introspectOperation(it.kqlOperation, it.returnType) }

        return __ImmutableType(
                kind = __TypeKind.OBJECT,
                name = "Mutation",
                description = "Root mutation type of schema",
                fields = fields,
                interfaces = emptyList(),
                possibleTypes = emptyList(),
                enumValues = emptyList(),
                inputFields = emptyList()
        )
    }

    private fun introspectQueryType(queries: Map<String, SchemaNode.Query<*>>) : __Type {
        val fields = queries.values
                .filter { it.kqlQuery.isNotIntrospection() }
                .map { introspectOperation(it.kqlOperation, it.returnType) }

        return __ImmutableType(
                kind = __TypeKind.OBJECT,
                name = "Query",
                description = "Root query type of schema",
                fields = fields,
                interfaces = emptyList(),
                possibleTypes = emptyList(),
                enumValues = emptyList(),
                inputFields = emptyList()
        )
    }

    private fun <T>introspectOperation(kqlOperation: KQLOperation<T>, returnType: SchemaNode.ReturnType) = __Field(
            kqlOperation.name,
            kqlOperation.description,
            introspectReturnType(returnType),
            introspectArguments(kqlOperation.argumentsDescriptor),
            kqlOperation.isDeprecated,
            kqlOperation.deprecationReason
    )

    private fun introspectType(type:SchemaNode.Type) : __Type {
        val cachedType = typeMap[type.kqlType.name]
        if(cachedType != null) return cachedType

        val kind: __TypeKind = determineKind(type)

        val enumValues : List<__EnumValue>? = introspectEnumValues(type)

        //TODO: persist information about inheritance tree in schemas
        val mutableType = __MutableType(
                kind = kind,
                name = type.kqlType.name,
                description = type.kqlType.description ?: "",
                enumValues = enumValues
        )

        typeMap[type.kqlType.name] = mutableType

        mutableType.mutableInterfaces += introspectInterfaces(kind, type)

        mutableType.mutablePossibleTypes += introspectPossibleTypes(kind, type)

        if(type.kqlType is KQLType.Object<*>){
            type.properties.values
                    .filterNot { it.kqlProperty.name.startsWith("__") }
                    .mapTo(mutableType.mutableFields) { introspectField(it) }

            type.unionProperties
                    .mapTo(mutableType.mutableFields) { introspectUnionField(it.value) }
        }

        if(type.kqlType is KQLType.Input<*>){
            type.properties.values.mapTo(mutableType.mutableInputFields) { property ->
                __InputValue(introspectReturnType(property.returnType), null, property.kqlProperty.name, null)
            }
        }

        return mutableType
    }

    private fun introspectPossibleTypes(kind: __TypeKind, type: SchemaNode.Type) : List<__Type> {
        return when(kind){
            __TypeKind.INTERFACE -> {
                if(type.kqlType is KQLType.Object<*>){
                    introspectInterfacePossibleTypes(type.kqlType)
                } else {
                    throw IllegalStateException("Unexpected $kind for ${type.kqlType}")
                }
            }
            __TypeKind.UNION -> {
                if(type.kqlType is KQLType.Union){
                    introspectUnionPossibleTypes(type.kqlType, type)
                } else {
                    throw IllegalStateException("Unexpected $kind for ${type.kqlType}")
                }
            }
            else -> emptyList()
        }
    }

    private fun introspectUnionPossibleTypes(kqlType: KQLType.Union, type: SchemaNode.Type): List<__Type> {
        return kqlType.members.map {
            val schemaType = schema.structure.queryTypes[it.starProjectedType]
                    ?: throw IllegalStateException("Cannot introspect union ${type.kqlType.name}, member type $it not found ")
            introspectType(schemaType)
        }
    }

    private fun introspectInterfacePossibleTypes(kqlType: KQLType.Object<*>): List<__Type> {
        val kClass = kqlType.kClass
        return schema.structure.queryTypes.filterKeys { kType ->
            val otherKClass = kType.jvmErasure
            otherKClass != kClass && otherKClass.isFinal && otherKClass.isSubclassOf(kClass)
        }.map { (_, schemaType) ->
            introspectType(schemaType)
        }
    }

    private fun introspectInterfaces(kind: __TypeKind, type: SchemaNode.Type): List<__Type> {
        return if (kind == __TypeKind.OBJECT) {
            if (type.kqlType is KQLType.Object<*>) {
                val kClass = type.kqlType.kClass
                schema.structure.queryTypes.filterKeys { kType ->
                    val otherKClass = kType.jvmErasure
                    otherKClass != kClass && otherKClass.isSuperclassOf(kClass)
                }.map { (_, schemaType) ->
                    introspectType(schemaType)
                }
            } else {
                throw IllegalStateException("Unexpected $kind for ${type.kqlType}")
            }
        } else emptyList<__Type>()
    }

    private fun determineKind(type: SchemaNode.Type) = when(type.kqlType) {
        is KQLType.Object<*> -> {
            if(type.kqlType.kClass.isFinal){
                __TypeKind.OBJECT
            } else {
                __TypeKind.INTERFACE
            }
        }
        is KQLType.Scalar<*> -> __TypeKind.SCALAR
        is KQLType.Enumeration<*> -> __TypeKind.ENUM
        is KQLType.Input<*> -> __TypeKind.INPUT_OBJECT
        is KQLType.Union -> __TypeKind.UNION
        else -> throw IllegalStateException("Unexpected KQLType: ${type.kqlType}")
    }

    private fun introspectField(field : SchemaNode.Property): __Field {
        val inputValues = if (field.kqlProperty is KQLProperty.Function<*>){
            introspectArguments(field.kqlProperty.argumentsDescriptor)
        } else{
            emptyList()
        }

        return __Field (
                field.kqlProperty.name,
                field.kqlProperty.description,
                introspectReturnType(field.returnType),
                inputValues,
                field.kqlProperty.isDeprecated,
                field.kqlProperty.deprecationReason
        )
    }

    private fun introspectUnionField(field : SchemaNode.UnionProperty) = __Field (
            field.kqlProperty.name,
            field.kqlProperty.description,
            introspectUnionType(field.kqlProperty.union),
            introspectArguments(field.kqlProperty.argumentsDescriptor),
            field.kqlProperty.isDeprecated,
            field.kqlProperty.deprecationReason
    )

    private fun introspectUnionType(union: KQLType.Union): __Type {
        return typeMap.getOrPut(union.name){
            __ImmutableType(
                    kind = __TypeKind.UNION,
                    name = union.name,
                    description = union.description ?: "",
                    fields = emptyList(),
                    interfaces = emptyList(),
                    possibleTypes = emptyList(),
                    enumValues = emptyList(),
                    inputFields = emptyList()
            )
        }
    }

    private fun introspectEnumValues(type: SchemaNode.Type) = when(type.kqlType){
        is KQLType.Enumeration<*> -> type.kqlType.values.map {
            __EnumValue(it.name, it.description, it.isDeprecated, it.deprecationReason)
        }
        else -> null
    }

    private fun introspectArguments(argumentsDescriptor: Map<String, KType>): List<__InputValue>{
        return argumentsDescriptor.entries.map { (name, kType) ->
            __InputValue(introspectInputType(kType), null, name, null)
        }
    }

    private fun introspectInputType(kType: KType) : __Type {
        val isCollection = kType.jvmErasure.isCollection()

        val lookupKType = if(isCollection){
            kType.arguments.first().type?.withNullability(false)
                    ?: throw SchemaException("Failed to introspect collection input type $kType")
        } else {
            kType.withNullability(false)
        }

        val schemaType = schema.structure.inputTypes[lookupKType]
                ?: throw IllegalStateException("Failed to introspect input type $kType")

        var type = introspectType(schemaType)

        if(isCollection && !(kType.arguments.first().type?.isMarkedNullable ?: true)){
            type = __NonNull(type)
        }
        if(isCollection){
            type = __List(type)
        }
        if(!kType.isMarkedNullable){
            type = __NonNull(type)
        }
        return type
    }

    private fun introspectReturnType(returnType: SchemaNode.ReturnType) : __Type {
        var type = introspectType(returnType.type)
        if(returnType.isCollection && returnType.areEntriesNotNullable){
            type = __NonNull(type)
        }
        if(returnType.isCollection){
            type = __List(type)
        }
        if(returnType.isNotNullable){
            type = __NonNull(type)
        }
        return type
    }

    fun KQLOperation<*>.isNotIntrospection() : Boolean = !name.startsWith("__")
}