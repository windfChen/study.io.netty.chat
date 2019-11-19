package com.windf.study.io.netty.chat.processor;

import com.alibaba.fastjson.JSONObject;
import com.windf.study.io.netty.chat.protocol.IMDecoder;
import com.windf.study.io.netty.chat.protocol.IMEncoder;
import com.windf.study.io.netty.chat.protocol.IMMessage;
import com.windf.study.io.netty.chat.protocol.IMP;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

public class MsgProcessor {
    //记录在线用户
    private static ChannelGroup onlineUsers = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    //定义一些扩展属性
    public static final AttributeKey<String> NICK_NAME = AttributeKey.valueOf("nickName");
    public static final AttributeKey<String> IP_ADDR = AttributeKey.valueOf("ipAddr");
    public static final AttributeKey<JSONObject> ATTRS = AttributeKey.valueOf("attrs");
    public static final AttributeKey<String> FROM = AttributeKey.valueOf("from");


    //自定义解码器
    private IMDecoder decoder = new IMDecoder();
    //自定义编码器
    private IMEncoder encoder = new IMEncoder();

    public void sendMsg(Channel client, String message) {
        sendMsg(client, decoder.decode(message));
    }

    public void sendMsg(Channel client, IMMessage request) {

        if (request == null) {
            return;
        }

        String addr = request.getAddr();
        // 用户登录
        if (IMP.LOGIN.getName().equals(request.getCmd())) {
            client.attr(NICK_NAME).getAndSet(request.getSender());
            client.attr(IP_ADDR).getAndSet(addr);
            client.attr(FROM).getAndSet(request.getTerminal());

            // 加入在线用户
            onlineUsers.add(client);

            // 发送给所有人
            for (Channel channel : onlineUsers) {
                IMMessage resp;
                if (channel == client) {
                    resp = new IMMessage(IMP.SYSTEM.getName(), sysTime(), onlineUsers.size(), "已经与服务器建立连接");
                } else {
                    resp = new IMMessage(IMP.SYSTEM.getName(), sysTime(), onlineUsers.size(), client.attr(NICK_NAME).get() + "加入");
                }

                if ("Console".equals(channel.attr(FROM))) {
                    channel.writeAndFlush(resp);
                } else {
                    String content = encoder.encode(resp);
                    channel.writeAndFlush(new TextWebSocketFrame(content));
                }
            }
        }

        // 发送消息
        else if (request.getCmd().equals(IMP.CHAT.getName())) {
            for (Channel channel : onlineUsers) {
                if (channel == client) {
                    request.setSender("you");
                } else {
                    request.setSender(client.attr(NICK_NAME).get());
                }

                request.setTime(sysTime());

                if("Console".equals(channel.attr(FROM).get()) & channel != client){
                    channel.writeAndFlush(request);
                } else {
                    String content = encoder.encode(request);
                    channel.writeAndFlush(new TextWebSocketFrame(content));
                }
            }
        }

        // 送花
        else if (IMP.FLOWER.getName().equals(request.getCmd())) {
            //正常送花
            for (Channel channel : onlineUsers) {
                if (channel == client) {
                    request.setSender("you");
                    request.setContent("你给大家送了一波鲜花雨");
                    setAttrs(client, "lastFlowerTime", sysTime());
                }else{
                    request.setSender(client.attr(NICK_NAME).get());
                    request.setContent(client.attr(NICK_NAME).get() + "送来一波鲜花雨");
                }

                request.setTime(sysTime());

                String content = encoder.encode(request);
                channel.writeAndFlush(new TextWebSocketFrame(content));
            }
        }

    }

    /**
     * 获取系统时间
     * @return
     */
    private Long sysTime(){
        return System.currentTimeMillis();
    }

    /**
     * 设置拓展属性
     * @param client
     * @param key
     * @param value
     */
    private void setAttrs(Channel client,String key,Object value){
        try{
            JSONObject json = client.attr(ATTRS).get();
            json.put(key, value);
            client.attr(ATTRS).set(json);
        }catch(Exception e){
            JSONObject json = new JSONObject();
            json.put(key, value);
            client.attr(ATTRS).set(json);
        }
    }
}
