package com.windf.study.io.netty.chat.server.handler;

import com.windf.study.io.netty.chat.processor.MsgProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.EventExecutorGroup;

public class WebSocketServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private MsgProcessor processor = new MsgProcessor();

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
        processor.sendMsg(channelHandlerContext.channel(), textWebSocketFrame.text());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        Channel channel = ctx.channel();
        System.out.println("WebSocket Client:" + channel.remoteAddress() + "异常");
        // 当出现异常就关闭连接
        cause.printStackTrace();
        ctx.close();
    }
}
