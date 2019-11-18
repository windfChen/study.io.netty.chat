package com.windf.study.io.netty.chat.server;

import com.windf.study.io.netty.chat.protocol.IMDecoder;
import com.windf.study.io.netty.chat.protocol.IMEncoder;
import com.windf.study.io.netty.chat.server.handler.TerminalServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

public class ChatServer {
    private int port = 10080;
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
                        }
                    })
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
