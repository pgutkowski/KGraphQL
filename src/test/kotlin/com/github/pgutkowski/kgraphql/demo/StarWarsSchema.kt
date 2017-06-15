package com.github.pgutkowski.kgraphql.demo

import com.github.pgutkowski.kgraphql.KGraphQL
import com.github.pgutkowski.kgraphql.schema.dsl.enum
import com.github.pgutkowski.kgraphql.schema.dsl.type

enum class Episode {
    NEWHOPE, EMPIRE, JEDI
}

interface Character {
    val id : String
    val name : String?
    val friends: List<Character>
    val appearsIn: Set<Episode>
}

data class Human (
        override val id: String,
        override val name: String?,
        override val friends: List<Character>,
        override val appearsIn: Set<Episode>,
        val homePlanet: String,
        val height: Double
) : Character

data class Droid (
        override val id: String,
        override val name: String?,
        override val friends: List<Character>,
        override val appearsIn: Set<Episode>,
        val primaryFunction : String
) : Character

val luke = Human("2000", "Luke Skywalker", emptyList(), Episode.values().toSet(), "Tatooine", 1.72)

val r2d2 = Droid("2001", "R2-D2", emptyList(), Episode.values().toSet(), "Astromech")

fun main(args: Array<String>) {
    val schema = KGraphQL.schema {

        configure {
            useDefaultPrettyPrinter = true
        }

        query("hero") {
            resolver {episode: Episode -> when(episode){
                Episode.NEWHOPE, Episode.JEDI -> r2d2
                Episode.EMPIRE -> luke
            }}
        }

        query("heroes") {
            resolver{ -> listOf(luke, r2d2)}
        }

        type<Droid>()
        type<Human>()

        enum<Episode>()
    }

    println(schema.execute("{hero(episode: JEDI){id, name, ... on Droid{primaryFunction} ... on Human{height}}}"))
    println(schema.execute("{heroes {id, name, ... on Droid{primaryFunction} ... on Human{height}}}"))
}