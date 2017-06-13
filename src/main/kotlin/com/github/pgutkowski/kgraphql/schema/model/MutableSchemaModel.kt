package com.github.pgutkowski.kgraphql.schema.model

import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.builtin.BuiltInType
import com.github.pgutkowski.kgraphql.schema.directive.Directive
import com.github.pgutkowski.kgraphql.defaultKQLTypeName

/**
 * Intermediate, mutable data structure used to prepare [SchemaModel]
 * Performs basic validation (names duplication etc.) when methods for adding schema components are invoked
 */
data class MutableSchemaModel (
        private val objects: ArrayList<KQLType.Object<*>> = arrayListOf<KQLType.Object<*>>(),
        private val queries: ArrayList<KQLQuery<*>> = arrayListOf<KQLQuery<*>>(),
        private val scalars: ArrayList<KQLType.Scalar<*>> = arrayListOf<KQLType.Scalar<*>>(
                BuiltInType.STRING,
                BuiltInType.BOOLEAN,
                BuiltInType.DOUBLE,
                BuiltInType.FLOAT,
                BuiltInType.INT
        ),
        private val mutations: ArrayList<KQLMutation<*>> = arrayListOf<KQLMutation<*>>(),
        private val enums: ArrayList<KQLType.Enumeration<*>> = arrayListOf<KQLType.Enumeration<*>>(),
        private val unions: ArrayList<KQLType.Union> = arrayListOf<KQLType.Union>(),
        private val directives: ArrayList<Directive> = arrayListOf(
                Directive.SKIP,
                Directive.INCLUDE
        )
) {

        val unionsMonitor : List<KQLType.Union>
                get() = unions

        fun toSchemaModel() : SchemaModel {
                val compiledObjects = ArrayList(this.objects)

                unions.forEach { union ->
                        union.members.forEach { member ->
                                if(scalars.any { it.kClass == member } || enums.any { it.kClass == member }){
                                        throw com.github.pgutkowski.kgraphql.schema.SchemaException(
                                                "The member types of a Union type must all be Object base types; " +
                                                        "Scalar, Interface and Union types may not be member types of a Union")
                                }

                                if(compiledObjects.none { it.kClass == member }){
                                        compiledObjects.add(KQLType.Object(member.defaultKQLTypeName(), member))
                                }
                        }
                }

                return SchemaModel(compiledObjects, queries, scalars, mutations, enums, unions, directives)
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

        fun <T : KQLObject>addType(type: T, target: ArrayList<T>, typeCategory: String, alternativeObjects: List<KQLType.Object<*>>? = null){
                if(type.name.startsWith("__")){
                        throw SchemaException("Type name starting with \"__\" are excluded for introspection system")
                }
                if(type.checkEqualName(alternativeObjects ?: objects, scalars, unions, enums)){
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
