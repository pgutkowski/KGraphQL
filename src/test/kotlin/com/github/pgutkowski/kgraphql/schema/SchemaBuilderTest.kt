package com.github.pgutkowski.kgraphql.schema

import com.github.pgutkowski.kgraphql.Actor
import com.github.pgutkowski.kgraphql.Context
import com.github.pgutkowski.kgraphql.FilmType
import com.github.pgutkowski.kgraphql.Id
import com.github.pgutkowski.kgraphql.KGraphQL
import com.github.pgutkowski.kgraphql.Scenario
import com.github.pgutkowski.kgraphql.context
import com.github.pgutkowski.kgraphql.defaultSchema
import com.github.pgutkowski.kgraphql.deserialize
import com.github.pgutkowski.kgraphql.expect
import com.github.pgutkowski.kgraphql.extract
import com.github.pgutkowski.kgraphql.schema.introspection.TypeKind
import com.github.pgutkowski.kgraphql.schema.scalar.StringScalarCoercion
import com.github.pgutkowski.kgraphql.schema.structure2.Field
import kotlinx.coroutines.delay
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
    fun `input value default value and description can be specified`(){
        val expectedDescription = "Int Argument"
        val expectedDefaultValue = 33
        val schema = defaultSchema {
            query("data"){
                resolver { int: Int -> int }.withArgs {
                    arg <Int> { name = "int"; defaultValue = expectedDefaultValue; description = expectedDescription }
                }
            }
        }

        val intArg = schema.queryType.fields?.find { it.name == "data" }?.args?.find { it.name == "int" }
        assertThat(intArg?.defaultValue, equalTo(expectedDefaultValue.toString()))
        assertThat(intArg?.description, equalTo(expectedDescription))

        val response = deserialize(schema.execute("{data}"))
        assertThat(response.extract<Int>("data/data"), equalTo(33))

        val introspection = deserialize(schema.execute("{__schema{queryType{fields{name, args{name, description, defaultValue}}}}}"))
        assertThat(introspection.extract<String>("data/__schema/queryType/fields[0]/args[0]/description"), equalTo(expectedDescription))
    }

    @Test
    fun `arg name must match exactly one of type property`(){
        expect<SchemaException>("Invalid input values on data: [intss]") {
            defaultSchema {
                query("data"){
                    resolver { int: Int, string: String? -> int }.withArgs {
                        arg <Int> { name = "intss"; defaultValue = 33 }
                    }
                }
            }
        }
    }

    @Test
    fun `arg name must be defined`(){
        expect<UninitializedPropertyAccessException>("lateinit property name has not been initialized") {
            defaultSchema {
                query("data"){
                    resolver { int: Int, string: String? -> int }.withArgs {
                        arg <Int> { defaultValue = 33 }
                    }
                }
            }
        }
    }

    data class UserData(val username: String, val stuff: String)

    @Test
    fun `client code can declare custom context class and use it in query resolver`(){
        val schema = KGraphQL.schema {
            query("name") {
                resolver { ctx: Context -> ctx.get<UserData>()?.username }
            }
        }

        val georgeName = "George"
        val response = deserialize(schema.execute("{name}", context { + UserData(georgeName, "STUFF") }))
        assertThat(response.extract<String>("data/name"), equalTo(georgeName))
    }

    @Test
    fun `client code can use context class in property resolver`(){
        val georgeName = "George"
        val schema = KGraphQL.schema {
            query("actor") {
                resolver { -> Actor("George", 23) }
            }

            type<Actor> {
                property<String>("nickname"){
                    resolver { _: Actor, ctx: Context -> "Hodor and ${ctx.get<UserData>()?.username}" }
                }

                transformation(Actor::name) { name: String, addStuff: Boolean?, ctx: Context ->
                    if(addStuff == true) name + ctx[UserData::class]?.stuff  else name
                }
            }
        }

        val context = context {
            + UserData(georgeName, "STUFF")
            inject("ADA")
        }
        val response = deserialize (schema.execute("{actor{ nickname, name(addStuff: true) }}", context))
        assertThat(response.extract<String>("data/actor/name"), equalTo("${georgeName}STUFF"))
        assertThat(response.extract<String>("data/actor/nickname"), equalTo("Hodor and $georgeName"))
    }

    @Test
    fun `context type cannot be part of schema`(){
        expect<SchemaException>("Context type cannot be part of schema") {
            KGraphQL.schema {
                query("name") {
                    resolver { ctx: Context -> ctx }
                }
            }
        }
    }

    @Test
    fun `There is clear message when query resolver is not present`(){
        expect<IllegalArgumentException>("resolver has to be specified for query [name]") {
            KGraphQL.schema {
                query("name") { -> "STUFF" }
            }
        }
    }

    class SixValues(val val1: Int = 1, val val2: String = "2", val val3: Int = 3, val val4: String = "4", val val5: Int = 5, val val6: String = "6")

    fun checkSixValuesSchema(schema: Schema) {
        val response = deserialize (schema.execute("{" +
            "queryWith1Param(val1: 2) { val1 }" +
            "queryWith2Params(val1: 2, val2: \"3\") { val1, val2 }" +
            "queryWith3Params(val1: 2, val2: \"3\", val3: 4) { val1, val2, val3 }" +
            "queryWith4Params(val1: 2, val2: \"3\", val3: 4, val4: \"5\") { val1, val2, val3, val4 }" +
            "queryWith5Params(val1: 2, val2: \"3\", val3: 4, val4: \"5\", val5: 6) { val1, val2, val3, val4, val5 }" +
            "queryWith6Params(val1: 2, val2: \"3\", val3: 4, val4: \"5\", val5: 6, val6: \"7\") { val1, val2, val3, val4, val5, val6 }" +
            "}"))
        assertThat(response.extract<Int>("data/queryWith1Param/val1"), equalTo(2))

        assertThat(response.extract<Int>("data/queryWith2Params/val1"), equalTo(2))
        assertThat(response.extract<String>("data/queryWith2Params/val2"), equalTo("3"))

        assertThat(response.extract<Int>("data/queryWith3Params/val1"), equalTo(2))
        assertThat(response.extract<String>("data/queryWith3Params/val2"), equalTo("3"))
        assertThat(response.extract<Int>("data/queryWith3Params/val3"), equalTo(4))

        assertThat(response.extract<Int>("data/queryWith4Params/val1"), equalTo(2))
        assertThat(response.extract<String>("data/queryWith4Params/val2"), equalTo("3"))
        assertThat(response.extract<Int>("data/queryWith4Params/val3"), equalTo(4))
        assertThat(response.extract<String>("data/queryWith4Params/val4"), equalTo("5"))

        assertThat(response.extract<Int>("data/queryWith5Params/val1"), equalTo(2))
        assertThat(response.extract<String>("data/queryWith5Params/val2"), equalTo("3"))
        assertThat(response.extract<Int>("data/queryWith5Params/val3"), equalTo(4))
        assertThat(response.extract<String>("data/queryWith5Params/val4"), equalTo("5"))
        assertThat(response.extract<Int>("data/queryWith5Params/val5"), equalTo(6))

        assertThat(response.extract<Int>("data/queryWith6Params/val1"), equalTo(2))
        assertThat(response.extract<String>("data/queryWith6Params/val2"), equalTo("3"))
        assertThat(response.extract<Int>("data/queryWith6Params/val3"), equalTo(4))
        assertThat(response.extract<String>("data/queryWith6Params/val4"), equalTo("5"))
        assertThat(response.extract<Int>("data/queryWith6Params/val5"), equalTo(6))
        assertThat(response.extract<String>("data/queryWith6Params/val6"), equalTo("7"))
    }

    @Test
    fun `Schema can contain resolvers with up to 6 parameters`() {
        val schema = KGraphQL.schema {
            query("queryWith1Param") {
                resolver { val1: Int ->
                    SixValues(val1)
                }
            }

            query("queryWith2Params") {
                resolver { val1: Int, val2: String ->
                    SixValues(val1, val2)
                }
            }

            query("queryWith3Params") {
                resolver { val1: Int, val2: String, val3: Int ->
                    SixValues(val1, val2, val3)
                }
            }

            query("queryWith4Params") {
                resolver { val1: Int, val2: String, val3: Int, val4: String ->
                    SixValues(val1, val2, val3, val4)
                }
            }

            query("queryWith5Params") {
                resolver { val1: Int, val2: String, val3: Int, val4: String, val5: Int ->
                    SixValues(val1, val2, val3, val4, val5)
                }
            }

            query("queryWith6Params") {
                resolver { val1: Int, val2: String, val3: Int, val4: String, val5: Int, val6: String ->
                    SixValues(val1, val2, val3, val4, val5, val6)
                }
            }
        }

        checkSixValuesSchema(schema)
    }

    @Test
    fun `Schema can contain suspend resolvers`() {
        val schema = KGraphQL.schema {
            query("queryWith1Param") {
                suspendResolver { val1: Int ->
                    delay(1)
                    SixValues(val1)
                }
            }

            query("queryWith2Params") {
                suspendResolver { val1: Int, val2: String ->
                    delay(1)
                    SixValues(val1, val2)
                }
            }

            query("queryWith3Params") {
                suspendResolver { val1: Int, val2: String, val3: Int ->
                    delay(1)
                    SixValues(val1, val2, val3)
                }
            }

            query("queryWith4Params") {
                suspendResolver { val1: Int, val2: String, val3: Int, val4: String ->
                    delay(1)
                    SixValues(val1, val2, val3, val4)
                }
            }

            query("queryWith5Params") {
                suspendResolver { val1: Int, val2: String, val3: Int, val4: String, val5: Int ->
                    delay(1)
                    SixValues(val1, val2, val3, val4, val5)
                }
            }

            query("queryWith6Params") {
                suspendResolver { val1: Int, val2: String, val3: Int, val4: String, val5: Int, val6: String ->
                    delay(1)
                    SixValues(val1, val2, val3, val4, val5, val6)
                }
            }
        }

        checkSixValuesSchema(schema)
    }
}