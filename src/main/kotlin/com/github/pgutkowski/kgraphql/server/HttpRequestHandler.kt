package com.github.pgutkowski.kgraphql.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.impl.DefaultSchema
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.AsciiString
import java.util.logging.Level
import java.util.logging.Logger

/**
 * TODO: refactor and split to two handlers : for queries and for API docs
 */
@ChannelHandler.Sharable
class HttpRequestHandler(val schema : DefaultSchema) : SimpleChannelInboundHandler<FullHttpRequest>() {

    val logger : Logger = Logger.getLogger( HttpRequestHandler::class.qualifiedName )

    val objectMapper = ObjectMapper()

    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        when{
            msg.uri().startsWith("/graphql?") -> handleQuery(ctx, msg)
            msg.uri().startsWith("/graphql/docs") -> handleDocQuery(ctx, msg)
            else -> exceptionCaught(ctx, IllegalArgumentException("Invalid path"))
        }
    }

    fun handleDocQuery(ctx: ChannelHandlerContext, msg: FullHttpRequest){
        val path = msg.uri().substring("/graphql/docs".length).split('/').filter(String::isNotBlank)
        writeResponse(ctx, schema.asHTML(path), AsciiString("text/html"))
    }

    private fun handleQuery(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        val queryParameters = QueryStringDecoder(msg.uri()).parameters()["query"] ?: throw IllegalArgumentException("Please specify query")
        val query = if (queryParameters.size == 1) queryParameters.first() else throw IllegalArgumentException("Please specify only one query")
        writeResponse(ctx, schema.handleRequest(query, null))
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.log(Level.INFO, cause.message)
        writeErrorMessage(cause, ctx)
    }

    private fun writeResponse(ctx: ChannelHandlerContext, response: String, contentType: AsciiString = HttpHeaderValues.APPLICATION_JSON) {

        val httpResponse = DefaultFullHttpResponse (
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(response.toByteArray())
        )

        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes())
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType)

        ctx.writeAndFlush(httpResponse)
    }
    private fun writeErrorMessage(cause: Throwable, ctx: ChannelHandlerContext) {
        val httpResponse = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.INTERNAL_SERVER_ERROR,
                Unpooled.wrappedBuffer(objectMapper.writeValueAsBytes(cause.message))
        )

        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes())
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE)
    }
}