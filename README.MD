# KGraphQL

[![Build Status](https://travis-ci.org/pgutkowski/KGraphQL.svg?branch=master)](https://travis-ci.org/pgutkowski/KGraphQL)
[![codebeat badge](https://codebeat.co/badges/b26d3c87-7cd1-4358-93cd-45d395669bdc)](https://codebeat.co/projects/github-com-pgutkowski-kgraphql-master)

KGraphQL is [Kotlin](https://kotlinlang.org/) implementation of [GraphQL](http://graphql.org/).

Obligatory Hello World example: 

```` kotlin
fun main(args : Array<String>) {
    val schema = KGraphQL.schema {
        query("hello") {
            resolver { name : String -> "Hello, $name" }
        }
    }

    //prints '{"data":{"hello":"Hello, Ted Mosby"}}'
    println(schema.execute("{hello(name : \"Ted Mosby\")}"))
}
````

## Building

To build KGraphQL you only need to have JDK8 installed. invoke

````
./gradlew build
````
To perform local build.

## Versioning

The versioning is following [Semantic Versioning](http://semver.org/)

## Links 

Specification : http://facebook.github.io/graphql/

## License

KGraphQL is Open Source software released under the [MIT license](https://opensource.org/licenses/MIT)
