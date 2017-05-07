package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.Actor
import com.github.pgutkowski.kgraphql.Director
import com.github.pgutkowski.kgraphql.Scenario
import com.github.pgutkowski.kgraphql.graph.DescriptorNode
import com.github.pgutkowski.kgraphql.graph.Graph
import com.github.pgutkowski.kgraphql.graph.branch
import com.github.pgutkowski.kgraphql.graph.leaf
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class SchemaDescriptorTest : BaseSchemaTest() {

    fun intersect(graph : Graph) : Graph = testedSchema.descriptor.intersect(graph)

    @Test
    fun simpleIntersection() {
        val intersected = intersect(Graph(
                branch("film", leaf("year"))
        ))

        val expected = Graph(
                branch("film", leaf("year"))
        )

        assertThat(intersected, equalTo(expected))
    }

    @Test
    fun implicitFieldsIntersection(){
        val intersected = intersect(Graph(leaf("film")))

        val expected = Graph(
                branch("film",
                        leaf("year"),
                        leaf("id"),
                        leaf("title"),
                        leaf("type"),
                        branch("director",
                                leaf("name"),
                                leaf("age"),
                                branch("favActors",
                                        leaf("age"),
                                        leaf("name")
                                )
                        )
                )
        )
        assertThat(intersected, equalTo(expected))
    }

    @Test
    fun implicitFieldWithInterface(){
        val intersected = intersect(Graph(leaf("randomPerson")))

        val expected = Graph(
                branch("randomPerson",
                        leaf("age"),
                        leaf("name"))
        )

        assertThat(intersected, equalTo(expected))
    }

    @Test
    fun testActorDescriptor(){
        val actorDescriptor = testedSchema.descriptor.get(Actor::class)
        assertThat(actorDescriptor, equalTo(Graph(
                DescriptorNode.leaf<Int>("age"),
                DescriptorNode.leaf<String>("name")
        )))
    }

    @Test
    fun testDirectorDescriptor(){
        val directorDescriptor = testedSchema.descriptor.get(Director::class)
        assertThat(directorDescriptor, equalTo(Graph(
                DescriptorNode.branch<Actor>("favActors",
                        DescriptorNode.leaf<Int>("age"),
                        DescriptorNode.leaf<String>("name")
                ),
                DescriptorNode.leaf<Int>("age"),
                DescriptorNode.leaf<String>("name")
        )))
    }

    @Test
    fun testNotRegisteredClassDescriptor(){
        val nullDescriptor = testedSchema.descriptor.get(DescriptorNode::class)
        assertThat(nullDescriptor, nullValue())
    }

    @Test
    fun testIgnoredProperty(){
        val scenarioDescriptor = testedSchema.descriptor.get(Scenario::class)!!
        assertThat(scenarioDescriptor["author"], nullValue())
    }
}