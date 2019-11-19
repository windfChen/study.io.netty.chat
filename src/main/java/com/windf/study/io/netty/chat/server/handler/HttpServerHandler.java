package com.windf.study.io.netty.chat.server.handler;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.EventExecutorGroup;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private URL baseURL = HttpServerHandler.class.getResource("");
    private final String WEB_ROOT = "webroot";

    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest request) throws Exception {
        String uri = request.getUri();

        HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
        boolean keepAlive = HttpHeaders.isKeepAlive(request);

        RandomAccessFile file = null;
        String page = uri.equals("/") ? "chat.html" : uri;
        try {
            file =	new RandomAccessFile(getResource(page), "r");

            String contextType = "text/html;";
            if(uri.endsWith(".css")){
                contextType = "text/css;";
            }else if(uri.endsWith(".js")){
                contextType = "text/javascript;";
            }else if(uri.toLowerCase().matches(".*\\.(jpg|png|gif)$")){
                String ext = uri.substring(uri.lastIndexOf("."));
                contextType = "image/" + ext;
            }
            response.headers().set(HttpHeaders.Names.CONTENT_TYPE, contextType + "charset=utf-8;");

            if (keepAlive) {
                response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, file.length());
                response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            }

            channelHandlerContext.write(response);
            channelHandlerContext.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));

            // 发送之后关闭连接
            ChannelFuture future = channelHandlerContext.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!keepAlive) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (IOException e) {
            e.printStackTrace();
            response.setStatus(HttpResponseStatus.NOT_FOUND);
            channelHandlerContext.write(response);
        } finally {
            // 关闭文件流
            try {
                file.close();
            } catch (Exception e) {

            }
        }

    }

    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable e) {
        Channel channel = channelHandlerContext.channel();
        System.out.println("Client:" + channel.remoteAddress()+"异常");
        e.printStackTrace();
        channelHandlerContext.close();
    }

    private File getResource(String fileName) throws Exception{
        String basePath = baseURL.toURI().toString();
        int start = basePath.indexOf("classes/");
        basePath = (basePath.substring(0,start) + "/" + "classes/").replaceAll("/+","/");

        String path = basePath + WEB_ROOT + "/" + fileName;
        path = !path.contains("file:") ? path : path.substring(5);
        path = path.replaceAll("//", "/");
        return new File(path);
    }
}
