# KGraphQL

[![Build Status](https://travis-ci.org/pgutkowski/KGraphQL.svg?branch=master)](https://travis-ci.org/pgutkowski/KGraphQL)
[![codebeat badge](https://codebeat.co/badges/b26d3c87-7cd1-4358-93cd-45d395669bdc)](https://codebeat.co/projects/github-com-pgutkowski-kgraphql-master)
[![Coverage Status](https://coveralls.io/repos/github/pgutkowski/KGraphQL/badge.svg?branch=master)](https://coveralls.io/github/pgutkowski/KGraphQL?branch=master)
[ ![Download](https://api.bintray.com/packages/pgutkowski/Maven/KGraphQL/images/download.svg) ](https://bintray.com/pgutkowski/Maven/KGraphQL/_latestVersion)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/KotlinBy/awesome-kotlin)

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
//KGraphQL#schema { } is entry point to create KGraphQL schema
val schema = KGraphQL.schema {

        //configure method allows you customize schema behaviour
        configure {
            useDefaultPrettyPrinter = true
        }

        //create query "hero" which returns instance of Character
        query("hero") {
            resolver {episode: Episode -> when(episode){
                Episode.NEWHOPE, Episode.JEDI -> r2d2
                Episode.EMPIRE -> luke
            }}
        }
    
        //create query "heroes" which returns list of luke and r2d2
        query("heroes") {
            resolver{ -> listOf(luke, r2d2)}
        }

        //kotlin classes need to be registered with "type" method 
        //to be included in created schema type system
        //class Character is automatically included, 
        //as it is return type of both created queries  
        type<Droid>()
        type<Human>()
        enum<Episode>()
    }
```
Now, we can query our schema:
```kotlin
//query for hero from episode JEDI and take id, name for any Character, and primaryFunction for Droid or height for Human
schema.execute("{hero(episode: JEDI){
                    id
                    name 
                    ... on Droid{primaryFunction} 
                    ... on Human{height}
                    }
                }")
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
//query for all heroes and take id, name for any Character, and primaryFunction for Droid or height for Human
schema.execute("{heroes {
                    id 
                    name 
                    ... on Droid{primaryFunction} 
                    ... on Human{height}
                    }
                }")
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
As stated by GraphQL specification, client receives only what is requested. No more, no less.

Detailed documentation can be found in [wiki](https://github.com/pgutkowski/KGraphQL/wiki). For more examples, see KGraphQL demo application: [KGraphQL-NBA2012](https://github.com/pgutkowski/KGraphQL-NBA2012)

## Using in your project
**_Please note that this library is still in experimental state and is subject to change._**

KGraphQL is pushed to bintray repository and also linked to JCenter. It requires kotlin compiler version 1.1.x and require kotlin runtime of the same version as a dependency.

### Maven

Add Bintray JCenter repository to <repositories> section:

```
<repository>
    <id>central</id>
    <url>http://jcenter.bintray.com</url>
</repository>
```

Add dependency:

```
<dependency>
  <groupId>com.github.pgutkowski</groupId>
  <artifactId>kgraphql</artifactId>
  <version>${KGraphQLVersion}</version>
</dependency>
```

And make sure that you use the right Kotlin version:

```
<properties>
    <kotlin.version>1.1.3</kotlin.version>
</properties>
```

### Gradle

Add Bintray JCenter repository:

```
repositories {
    jcenter()
}
```

Add dependencies (you can also add other modules that you need):

```
compile 'com.github.pgutkowski:kgraphql:${KGraphQLVersion}'
```

And make sure that you use the right Kotlin version:

```
buildscript {
    ext.kotlin_version = '1.1.3'
}
```

## Contributing

All contributions are welcome. Feel free to open issues and PRs!

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
