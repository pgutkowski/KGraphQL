package com.github.pgutkowski.kgraphql

import com.github.pgutkowski.kgraphql.schema.ScalarSupport


class TestClasses {

    class Film(val id : Id, val year: Int, val title: String, val director: Director)

    abstract class Person(val name: String, val age: Int)

    class Director(name : String, age: Int, val favActors: List<Actor>) : Person(name, age)

    class Actor(name : String, age: Int) : Person(name, age)

    class Id(val literal: String, val numeric: Int)

    class IdScalarSupport : ScalarSupport<Id> {
        override fun serialize(input: String): Id = Id(input.split(':')[0], input.split(':')[1].toInt())
        override fun deserialize(input: Id): String = "${input.literal}:${input.numeric}"
        override fun validate(input: String) : Boolean = input.isNotEmpty() && input.contains(':')
    }

    class FilmInput
}