package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.schema.SchemaException
import com.github.pgutkowski.kgraphql.schema.builtin.BuiltInType
import com.github.pgutkowski.kgraphql.schema.model.KQLMutation
import com.github.pgutkowski.kgraphql.schema.model.KQLObject
import com.github.pgutkowski.kgraphql.schema.model.KQLQuery
import com.github.pgutkowski.kgraphql.schema.model.KQLType
import com.github.pgutkowski.kgraphql.typeName

data class MutableSchemaModel(
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
        private val unions: ArrayList<KQLType.Union> = arrayListOf<KQLType.Union>()
) {

        val unionsMonitor : List<KQLType.Union>
                get() = unions

        fun toSchemaModel() : SchemaModel {
                val compiledObjects = ArrayList(this.objects)

                unions.forEach { union ->
                        union.members.forEach { member ->
                                if(scalars.any { it.kClass == member } || enums.any { it.kClass == member }){
                                        throw SchemaException(
                                                "The member types of a Union type must all be Object base types; " +
                                                        "Scalar, Interface and Union types may not be member types of a Union")
                                }

                                if(compiledObjects.none { it.kClass == member }){
                                        compiledObjects.add(KQLType.Object(member.typeName(), member))
                                }
                        }
                }

                return SchemaModel(compiledObjects, queries, scalars, mutations, enums, unions)
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
                if(type.checkEqualName(alternativeObjects ?: objects, scalars, unions, enums)){
                        throw SchemaException("Cannot add $typeCategory with duplicated name ${type.name}")
                }
                target.add(type)
        }

        fun KQLObject.checkEqualName(vararg collections: List<KQLObject>) : Boolean {
                return collections.fold(false, { acc, list -> acc || list.any { it.equalName(this) } })
        }

        fun KQLObject.equalName(other: KQLObject): Boolean {
                return this.name.equals(other.name, true)
        }
}
