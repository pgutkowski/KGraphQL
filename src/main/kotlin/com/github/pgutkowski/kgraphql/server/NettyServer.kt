package com.github.pgutkowski.kgraphql.server

import com.github.pgutkowski.kgraphql.schema.Schema
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LoggingHandler
import java.util.logging.Logger

class NettyServer {

    companion object {

        private val logger : Logger = Logger.getLogger( NettyServer::class.qualifiedName )

        fun run(schema : Schema) {

            val workerGroup = NioEventLoopGroup()
            val port = 8080

            try {
                val channel = ServerBootstrap()
                        .channel(NioServerSocketChannel::class.java)
                        .localAddress(port)
                        .group(workerGroup)
                        .handler(LoggingHandler())
                        .childHandler(NettyServerPipelineConfig(HttpRequestHandler(schema)))
                        .option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .bind().syncUninterruptibly().channel()

                channel.closeFuture().syncUninterruptibly()
            } finally {
                workerGroup.shutdownGracefully()
            }
        }
    }
}