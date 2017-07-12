package com.github.pgutkowski.kgraphql.schema

import com.github.pgutkowski.kgraphql.Actor
import com.github.pgutkowski.kgraphql.FilmType
import com.github.pgutkowski.kgraphql.Id
import com.github.pgutkowski.kgraphql.KGraphQL
import com.github.pgutkowski.kgraphql.Scenario
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import com.github.pgutkowski.kgraphql.schema.introspection.TypeKind
import com.github.pgutkowski.kgraphql.schema.scalar.StringScalarCoercion
import com.github.pgutkowski.kgraphql.schema.structure2.Field
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.*

/**
 * Tests for SchemaBuilder behaviour, not request execution
 */
class SchemaBuilderTest {
    @Test
    fun `DSL created UUID scalar support`(){

        val testedSchema = defaultSchema {
            stringScalar<UUID> {
                description = "unique identifier of object"
                deserialize = { uuid : String -> UUID.fromString(uuid) }
                serialize = UUID::toString
            }
        }

        val uuidScalar = testedSchema.model.scalars[UUID::class]!!.coercion as StringScalarCoercion<UUID>
        val testUuid = UUID.randomUUID()
        MatcherAssert.assertThat(uuidScalar.serialize(testUuid), CoreMatchers.equalTo(testUuid.toString()))
        MatcherAssert.assertThat(uuidScalar.deserialize(testUuid.toString()), CoreMatchers.equalTo(testUuid))
    }

    @Test
    fun `ignored property DSL`() {
        val testedSchema = defaultSchema {
            query("scenario") {
                resolver { -> Scenario(Id("GKalus", 234234), "Gamil Kalus", "TOO LONG") }
            }
            type<Scenario>{
                Scenario::author.ignore()
                Scenario::content.configure {
                    description = "Content is Content"
                    isDeprecated = false
                }
            }
        }

        val scenarioType = testedSchema.model.queryTypes[Scenario::class]
                ?: throw Exception("Scenario type should be present in schema")
        assertThat(scenarioType["author"], nullValue())
        assertThat(scenarioType["content"], notNullValue())
    }

    @Test
    fun `transformation DSL`() {
        val testedSchema = defaultSchema {
            query("scenario") {
                resolver { -> Scenario(Id("GKalus", 234234),"Gamil Kalus", "TOO LONG") }
            }
            type<Scenario> {

                transformation(Scenario::content, { content: String, capitalized : Boolean? ->
                    if(capitalized == true) content.capitalize() else content
                })
            }
        }
        val scenarioType = testedSchema.model.queryTypes[Scenario::class]
                ?: throw Exception("Scenario type should be present in schema")
        assertThat(scenarioType.kind, equalTo(TypeKind.OBJECT))
        assertThat(scenarioType["content"], notNullValue())
    }

    @Test
    fun `extension property DSL`(){
        val testedSchema = defaultSchema {

            query("scenario") {
                resolver { -> Scenario(Id("GKalus", 234234),"Gamil Kalus", "TOO LONG") }
            }

            type<Scenario> {
                property<String>("pdf") {
                    description = "link to pdf representation of scenario"
                    resolver { scenario : Scenario -> "http://scenarios/${scenario.id}" }
                }
            }
        }

        val scenarioType = testedSchema.model.queryTypes[Scenario::class]
                ?: throw Exception("Scenario type should be present in schema")

        assertThat(scenarioType.kind, equalTo(TypeKind.OBJECT))
        assertThat(scenarioType["pdf"], notNullValue())

    }

    @Test
    fun `union type DSL`(){
        val tested = defaultSchema {

            query("scenario") {
                resolver { -> Scenario(Id("GKalus", 234234),"Gamil Kalus", "TOO LONG") }
            }

            val linked = unionType("Linked") {
                type<Actor>()
                type<Scenario>()
            }

            type<Scenario> {
                unionProperty("pdf") {
                    returnType = linked
                    description = "link to pdf representation of scenario"
                    resolver { scenario : Scenario ->
                        if(scenario.author.startsWith("Gamil")){
                            Scenario(Id("ADD", 22), "gambino", "nope")
                        } else{
                            Actor("Chance", 333)
                        }
                    }
                }
            }
        }

        val scenarioType = tested.model.queryTypes[Scenario::class]
                ?: throw Exception("Scenario type should be present in schema")

        val unionField = scenarioType["pdf"]
        assertThat(unionField, notNullValue())
        assertThat(unionField, instanceOf(Field.Union::class.java))
    }

    @Test
    fun `circular dependency extension property`(){
        val tested = defaultSchema {
            query("actor") {
                resolver { -> Actor("Little John", 44) }
            }

            type<Actor> {
                property<Actor>("linked") {
                    resolver { _ -> Actor("BIG John", 3234) }
                }
            }
        }

        val actorType = tested.model.queryTypes[Actor::class]
                ?: throw Exception("Scenario type should be present in schema")
        assertThat(actorType.kind, equalTo(TypeKind.OBJECT))
        val property = actorType["linked"] ?: throw Exception("Actor should have ext property 'linked'")
        assertThat(property, notNullValue())
        assertThat(property.returnType.unwrapped().name, equalTo("Actor"))
    }

    @Test
    fun ` _ is allowed as receiver argument name`(){
        val schema = defaultSchema {
            query("actor") {
                resolver { -> Actor("Boguś Linda", 4343) }
            }

            type<Actor>{
                property<List<String>>("favDishes") {
                    resolver { _: Actor, size: Int->
                        listOf("steak", "burger", "soup", "salad", "bread", "bird").take(size)
                    }
                }
            }
        }

        deserialize(schema.execute("{actor{favDishes(size: 2)}}"))
    }

    @Test
    fun `Custom type name`(){
        val schema = defaultSchema {
            query("actor") {
                resolver { type: FilmType -> Actor("Boguś Linda $type", 4343)  }
            }

            enum<FilmType> {
                name = "TYPE"
            }
        }

        val result = deserialize(schema.execute("query(\$type : TYPE = FULL_LENGTH){actor(type: \$type){name}}"))
        assertThat(result.extract<String>("data/actor/name"), equalTo("Boguś Linda FULL_LENGTH"))
    }

    private data class LambdaWrapper(val lambda : () -> Int)

    @Test
    fun `function properties cannot be handled`(){
        expect<SchemaException>("Generic types are not supported by GraphQL, found () -> kotlin.Int"){
            KGraphQL.schema {
                query("lambda"){
                    resolver { -> LambdaWrapper({ 1 }) }
                }
            }
        }
    }

    class InputOne(val string:  String)

    class InputTwo(val one : InputOne)

    @Test
    fun `Schema should map input types`(){
        val schema = defaultSchema {
            inputType<InputTwo>()
        }

        assertThat(schema.inputTypeByKClass(InputOne::class), notNullValue())
        assertThat(schema.inputTypeByKClass(InputTwo::class), notNullValue())
    }

    @Test
    fun `Schema should infer input types from resolver functions`(){
        val schema = defaultSchema {
            query("sample") {
                resolver { i: InputTwo -> "SUCCESS" }
            }
        }

        assertThat(schema.inputTypeByKClass(InputOne::class), notNullValue())
        assertThat(schema.inputTypeByKClass(InputTwo::class), notNullValue())
    }

    @Test
    fun `generic types are not supported`(){
        expect<SchemaException>("Generic types are not supported by GraphQL, found kotlin.Pair<kotlin.Int, kotlin.String>"){
            defaultSchema {
                query("data"){
                    resolver { int: Int, string: String -> int to string }
                }
            }
        }
    }

    @Test
    fun `input value default value can be specified`(){
        val schema = defaultSchema {
            query("data"){
                resolver { int: Int, string: String? -> int }.withArgs {
                    arg <Int> { name = "int"; defaultValue = 33 }
                }
            }
        }

        val intArg = schema.queryType.fields?.find { it.name == "data" }?.args?.find { it.name == "int" }
        assertThat(intArg?.defaultValue, equalTo("33"))

        val response = deserialize(schema.execute("{data}"))
        assertThat(response.extract<Int>("data/data"), equalTo(33))
    }
}