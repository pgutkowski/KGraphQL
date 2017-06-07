# KGraphQL

[![Build Status](https://travis-ci.org/pgutkowski/KGraphQL.svg?branch=master)](https://travis-ci.org/pgutkowski/KGraphQL)
[![codebeat badge](https://codebeat.co/badges/b26d3c87-7cd1-4358-93cd-45d395669bdc)](https://codebeat.co/projects/github-com-pgutkowski-kgraphql-master)

KGraphQL is [Kotlin](https://kotlinlang.org/) implementation of [GraphQL](http://graphql.org/). It provides rich DSL to setup GraphQL schema. 

## Introduction

As example, let's partially reproduce part of Star Wars schema from [official GraphQL tutorial](http://graphql.org/learn/queries/). First, we need to define our domain model, by plain kotlin classes: 

```kotlin
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
```
Next, we define our data 
``` kotlin
val luke = Human("2000", "Luke Skywalker", emptyList(), Episode.values().toSet(), "Tatooine", 1.72)

val r2d2 = Droid("2001", "R2-D2", emptyList(), Episode.values().toSet(), "Astromech")
```

Then, we can create schema:

``` kotlin
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
```
Now, we can query our schema:
```kotlin
schema.execute("{hero(episode: JEDI){id, name, ... on Droid{primaryFunction} ... on Human{height}}}")
```
Returns:
```json
{
  "data" : {
    "hero" : {
      "id" : "2001",
      "name" : "R2-D2",
      "primaryFunction" : "Astromech"
    }
  }
}
```
Query for all heroes:
```kotlin
schema.execute("{heroes {id, name, ... on Droid{primaryFunction} ... on Human{height}}}")
```
Returns:
```json
{
  "data" : {
    "heroes" : [ {
      "id" : "2000",
      "name" : "Luke Skywalker",
      "height" : 1.72
    }, {
      "id" : "2001",
      "name" : "R2-D2",
      "primaryFunction" : "Astromech"
    } ]
  }
}
```
More specific documentation is available in `API.MD`

## Building

To build KGraphQL you only need to have JDK8 installed. invoke

``` bash
./gradlew build
```
To perform local build.

## Versioning

The versioning is following [Semantic Versioning](http://semver.org/)

## Links 

Specification : http://facebook.github.io/graphql/

## License

KGraphQL is Open Source software released under the [MIT license](https://opensource.org/licenses/MIT)
