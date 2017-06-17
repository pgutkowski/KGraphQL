package com.github.pgutkowski.kgraphql

import com.github.pgutkowski.kgraphql.schema.scalar.StringScalarCoercion


class Film(val id : Id, val year: Int, val title: String, val director: Director, val type: FilmType = FilmType.FULL_LENGTH)

abstract class Person(val name: String, val age: Int)

class Director(name : String, age: Int, val favActors: List<Actor>) : Person(name, age)

class Actor(name : String, age: Int) : Person(name, age)

class Id(val literal: String, val numeric: Int)

class IdScalarSupport : StringScalarCoercion<Id> {
    override fun serialize(instance: Id): String = "${instance.literal}:${instance.numeric}"

    override fun deserialize(raw: String): Id = Id(raw.split(':')[0], raw.split(':')[1].toInt())
}

enum class FilmType { FULL_LENGTH, SHORT_LENGTH }

class Scenario(val id : Id, val author : String, val content : String)