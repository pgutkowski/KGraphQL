# API

Entry point is `KGraphQL.schema` function, that accepts `SchemaBuilder.() -> Unit` lambda.

## SchemaBuilder
`SchemaBuilder` exposes ability to configure created schema and configure it. Its methods accept closures to configure contained DSL items.

### query { }
`query` allows to create resolver for query operation. it supports fields:

| name | description  |
| ---- | -----------  |
| name | name of query operation |
| description | description of operation displayed in documentation |
| resolver | function executed to resolve query result | 

*Example*
``` kotlin
query("hero"){
    description = "returns formatted name of R2-D2"
    resolver { -> "R2-D2" } 
}
```
This example adds query with name `hero`, which returns string: `R2-D2`

### mutation { }
`mutation` allows to create resolver for mutation operation. it supports fields:

| name | description  |
| ---- | -----------  |
| name | name of mutation operation |
| description | description of operation displayed in documentation |
| resolver | function executed to resolve mutation result | 

*Example*
``` kotlin
mutation("createHero"){
    description = "Creates hero with specified name"
    resolver { name : String -> name } 
}
```
This example adds mutation with name `createHero`, which returns passed name.

### type { }
`type<T>()` adds type T to KGraphQL schema. Schema is constructed with operations as roots. Branches are mapped based on classes property references.

## TypeDSL
`TypeDSL` exposes methods to customize how kotlin class is represented in `KGraphQL` schema.

TO BE CONTINUED.
