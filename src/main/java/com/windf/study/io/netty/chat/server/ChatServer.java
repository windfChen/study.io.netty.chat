package com.windf.study.io.netty.chat.server;

import com.windf.study.io.netty.chat.protocol.IMDecoder;
import com.windf.study.io.netty.chat.protocol.IMEncoder;
import com.windf.study.io.netty.chat.server.handler.HttpServerHandler;
import com.windf.study.io.netty.chat.server.handler.TerminalServerHandler;
import com.windf.study.io.netty.chat.server.handler.WebSocketServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class ChatServer {
    public void start(int port) {
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline channelPipeline = socketChannel.pipeline();

                            // 自定义协议
                            channelPipeline.addLast(new IMDecoder());
                            channelPipeline.addLast(new IMEncoder());
                            channelPipeline.addLast(new TerminalServerHandler());

                            // http协议
                            channelPipeline.addLast(new HttpServerCodec());
                            // 主要是将同一个http请求或响应的多个消息对象变成一个 fullHttpRequest完整的消息对象
                            channelPipeline.addLast(new HttpObjectAggregator(64 * 1024));
                            // 主要用于处理大数据流
                            channelPipeline.addLast(new ChunkedWriteHandler());
                            channelPipeline.addLast(new HttpServerHandler());

                            // webSocket协议
                            channelPipeline.addLast(new WebSocketServerProtocolHandler("/im"));
                            channelPipeline.addLast(new WebSocketServerHandler());

                        }
                    });
            ChannelFuture f = serverBootstrap.bind(port).sync();
            System.out.println("服务已启动,监听端口" + port);
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ChatServer().start(80);
    }
}
