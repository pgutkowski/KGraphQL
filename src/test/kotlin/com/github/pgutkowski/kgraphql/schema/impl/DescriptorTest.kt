package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.TestClasses
import com.github.pgutkowski.kgraphql.graph.DescriptorNode
import com.github.pgutkowski.kgraphql.graph.DescriptorNode.Companion.branch
import com.github.pgutkowski.kgraphql.graph.DescriptorNode.Companion.leaf
import com.github.pgutkowski.kgraphql.graph.Graph
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test


class DescriptorTest : BaseSchemaTest() {
    @Test
    fun testActorDescriptor(){
        val actorDescriptor = testedSchema.descriptor.get(TestClasses.Actor::class)
        assertThat(actorDescriptor, equalTo(Graph(
                leaf<Int>("age"),
                leaf<String>("name")
        )))
    }

    @Test
    fun testDirectorDescriptor(){
        val directorDescriptor = testedSchema.descriptor.get(TestClasses.Director::class)
        assertThat(directorDescriptor, equalTo(Graph(
                branch<TestClasses.Actor>("favActors",
                        leaf<Int>("age"),
                        leaf<String>("name")
                ),
                leaf<Int>("age"),
                leaf<String>("name")
        )))
    }

    @Test
    fun testInvalidClassDescriptor(){
        val nullDescriptor = testedSchema.descriptor.get(DescriptorNode::class)
        assertThat(nullDescriptor, nullValue())
    }
}