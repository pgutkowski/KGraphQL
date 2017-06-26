package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.defaultKQLTypeName
import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.builtin.BUILT_IN_TYPE
import com.github.pgutkowski.kgraphql.schema.directive.Directive
import com.github.pgutkowski.kgraphql.schema.directive.DirectiveLocation
import com.github.pgutkowski.kgraphql.schema.introspection.__List
import com.github.pgutkowski.kgraphql.schema.introspection.__NonNull
import com.github.pgutkowski.kgraphql.schema.introspection.__Schema
import com.github.pgutkowski.kgraphql.schema.introspection.__TypeKind
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Intermediate, mutable data structure used to prepare [SchemaDefinition]
 * Performs basic validation (names duplication etc.) when methods for adding schema components are invoked
 */
data class MutableSchemaDefinition (
        private val objects: ArrayList<KQLType.Object<*>> = arrayListOf(
                KQLType.Object(__Schema::class.defaultKQLTypeName(), __Schema::class)
        ),
        private val queries: ArrayList<KQLQuery<*>> = arrayListOf(),
        private val scalars: ArrayList<KQLType.Scalar<*>> = arrayListOf(
                BUILT_IN_TYPE.STRING,
                BUILT_IN_TYPE.BOOLEAN,
                BUILT_IN_TYPE.DOUBLE,
                BUILT_IN_TYPE.FLOAT,
                BUILT_IN_TYPE.INT
        ),
        private val mutations: ArrayList<KQLMutation<*>> = arrayListOf(),
        private val enums: ArrayList<KQLType.Enumeration<*>> = arrayListOf(
                KQLType.Enumeration (
                        __TypeKind::class.defaultKQLTypeName(),
                        __TypeKind::class,
                        enumValues<__TypeKind>().map { KQLEnumValue(it) }
                ),
                KQLType.Enumeration (
                        "__DirectiveLocation",
                        DirectiveLocation::class,
                        enumValues<DirectiveLocation>().map { KQLEnumValue(it) }
                )
        ),
        private val unions: ArrayList<KQLType.Union> = arrayListOf(),
        private val directives: ArrayList<Directive> = arrayListOf(
                Directive.SKIP,
                Directive.INCLUDE
        ),
        private val inputObjects: ArrayList<KQLType.Input<*>> = arrayListOf()
) {

        val unionsMonitor : List<KQLType.Union>
                get() = unions

        fun toSchemaModel(introspectionSchema: __Schema) : SchemaDefinition {
                val compiledObjects = ArrayList(this.objects)

                addQuery(KQLQuery("__schema", FunctionWrapper.on<__Schema> { introspectionSchema }))

                addQuery(KQLQuery("__type", FunctionWrapper.on {
                        name : String -> introspectionSchema.findTypeByName(name)
                }))

                unions.forEach { union ->
                        if(union.members.isEmpty()){
                                throw SchemaException("A Union type must define one or more unique member types")
                        }
                        union.members.forEach { member ->
                                validateUnionMember(union, member, compiledObjects)
                        }
                }

                return SchemaDefinition(compiledObjects, queries, scalars, mutations, enums, unions, directives, inputObjects)
        }

        private fun validateUnionMember(union: KQLType.Union,
                                        member: KClass<*>,
                                        compiledObjects: ArrayList<KQLType.Object<*>>) {
                if (scalars.any { it.kClass == member } || enums.any { it.kClass == member }) {
                        throw SchemaException(
                                "The member types of a Union type must all be Object base types; " +
                                        "Scalar, Interface and Union types may not be member types of a Union")
                }

                if (member.isSubclassOf(Collection::class)) {
                        throw SchemaException("Collection may not be member type of a Union '${union.name}'")
                }

                if (member.isSubclassOf(Map::class)) {
                        throw SchemaException("Map may not be member type of a Union '${union.name}'")
                }

                if (compiledObjects.none { it.kClass == member }) {
                        compiledObjects.add(KQLType.Object(member.defaultKQLTypeName(), member))
                }
        }

        fun addQuery(query : KQLQuery<*>){
                if(query.checkEqualName(queries)){
                        throw SchemaException("Cannot add query with duplicated name ${query.name}")
                }
                queries.add(query)
        }

        fun addMutation(mutation : KQLMutation<*>){
                if(mutation.checkEqualName(mutations)){
                        throw SchemaException("Cannot add mutation with duplicated name ${mutation.name}")
                }
                mutations.add(mutation)
        }

        fun addScalar(scalar: KQLType.Scalar<*>) = addType(scalar, scalars, "Scalar")

        fun addEnum(enum: KQLType.Enumeration<*>) = addType(enum, enums, "Enumeration")

        fun addObject(objectType: KQLType.Object<*>) = addType(objectType, objects, "Object")

        fun addUnion(union: KQLType.Union) = addType(union, unions, "Union")

        fun addInputObject(input : KQLType.Input<*>) = addType(input, inputObjects, "Input")

        fun <T : KQLObject>addType(type: T, target: ArrayList<T>, typeCategory: String){
                if(type.name.startsWith("__")){
                        throw SchemaException("Type name starting with \"__\" are excluded for introspection system")
                }
                if(type.checkEqualName(objects, scalars, unions, enums)){
                        throw SchemaException("Cannot add $typeCategory type with duplicated name ${type.name}")
                }
                target.add(type)
        }

        private fun KQLObject.checkEqualName(vararg collections: List<KQLObject>) : Boolean {
                return collections.fold(false, { acc, list -> acc || list.any { it.equalName(this) } })
        }

        private fun KQLObject.equalName(other: KQLObject): Boolean {
                return this.name.equals(other.name, true)
        }
}
