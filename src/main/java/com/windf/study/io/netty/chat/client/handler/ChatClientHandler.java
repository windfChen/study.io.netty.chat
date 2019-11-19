package com.windf.study.io.netty.chat.client.handler;

import com.windf.study.io.netty.chat.protocol.IMMessage;
import com.windf.study.io.netty.chat.protocol.IMP;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatClientHandler extends SimpleChannelInboundHandler<IMMessage> {
    private ChannelHandlerContext channelHandlerContext;
    private String nickName;

    public ChatClientHandler(String nickName) {
        this.nickName = nickName;
    }

    /**
     * tcp链路建立成功后调用
     */
    @Override
    public void channelActive(ChannelHandlerContext channelHandlerContext) throws IOException {
        IMMessage message = new IMMessage(IMP.LOGIN.getName(),"Console",System.currentTimeMillis(),this.nickName);
        sendMsg(message);
        session();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, IMMessage imMessage) throws Exception {
        this.channelHandlerContext = channelHandlerContext;
        System.out.println((null == imMessage.getSender() ? "" : (imMessage.getSender() + ":")) + removeHtmlTag(imMessage.getContent()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("与服务器断开连接:"+cause.getMessage());
        ctx.close();
    }

    /**
     * 启动客户端控制台
     * @throws IOException
     */
    private void session() throws IOException {
        new Thread(){
            public void run(){
                System.out.println(nickName + ",你好，请在控制台输入对话内容");
                IMMessage message = null;
                Scanner scanner = new Scanner(System.in);
                do{
                    if(scanner.hasNext()){
                        String input = scanner.nextLine();
                        if("exit".equals(input)){
                            message = new IMMessage(IMP.LOGOUT.getName(),"Console",System.currentTimeMillis(),nickName);
                        }else{
                            message = new IMMessage(IMP.CHAT.getName(),System.currentTimeMillis(),nickName,input);
                        }
                    }
                }
                while (sendMsg(message));
                scanner.close();
            }
        }.start();
    }

    /**
     * 发送消息
     * @param msg
     * @return
     */
    private boolean sendMsg(IMMessage msg){
        channelHandlerContext.channel().writeAndFlush(msg);
        System.out.println("继续输入开始对话...");
        return msg.getCmd().equals(IMP.LOGOUT) ? false : true;
    }

    private String removeHtmlTag(String htmlStr) {
        String regEx_script = "<script[^>]*?>[\\s\\S]*?<\\/script>"; //定义script的正则表达式
        String regEx_style = "<style[^>]*?>[\\s\\S]*?<\\/style>"; //定义style的正则表达式
        String regEx_html = "<[^>]+>"; //定义HTML标签的正则表达式

        Pattern p_script = Pattern.compile(regEx_script, Pattern.CASE_INSENSITIVE);
        Matcher m_script = p_script.matcher(htmlStr);
        htmlStr = m_script.replaceAll(""); //过滤script标签

        Pattern p_style = Pattern.compile(regEx_style, Pattern.CASE_INSENSITIVE);
        Matcher m_style = p_style.matcher(htmlStr);
        htmlStr = m_style.replaceAll(""); //过滤style标签

        Pattern p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
        Matcher m_html = p_html.matcher(htmlStr);
        htmlStr = m_html.replaceAll(""); //过滤html标签

        return htmlStr.trim(); //返回文本字符串
    }
}
