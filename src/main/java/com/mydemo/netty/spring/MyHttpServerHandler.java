package com.mydemo.netty.spring;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mydemo.utils.RedisUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author 石玉龙 at 2024/12/23 14:26
 */
//网上很多说这个@Sharable跟ChannelHandler是单例有关，其实没有什么关系。
// ChannelHandler是否为单例取决于使用者添加的是否为单例。和开发者的行为有关。
// 在管道中（SocketChannel.pipeline().addLast）添加时用spring默认的单例方式添加（交给spring管理）为单例，则需要@ChannelHandler.Sharable注解，
// 如果通过new对象的方式添加则不为单例，则不需要此注解
// 但是如果你想使用单例的ChannelHandler添加到ChannelPipeline中那么就需要用@Sharable进行修饰。
//原文链接：https://blog.csdn.net/weixin_38598961/article/details/130321683
@Slf4j
@Component
@ChannelHandler.Sharable
public class MyHttpServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {


    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ChannelContextUtil channelContextUtil;

//    /**
//     * 通道就绪后执行，用于用户的初始化
//     * @param ctx
//     * @throws Exception
//     */
//    @Override
//    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("有新的连接加入");
//    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //删除缓存中的用户
        channelContextUtil.removeUser(ctx.channel());
    }

    //    /**
//     * 将连接放入map中
//     * @param ctx
//     * @throws Exception
//     */
//    @Override
//    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("handlerAdded");
//    }
//
//    @Override
//    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("handlerRemoved");
//    }





    //channelRead0 读取客户端数据
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        Channel channel = ctx.channel();
        Attribute<String> attr = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        String userId = attr.get();
        String text = msg.text();
        log.info("userId:{},msg:{}", userId, text);
        MessageSendDTO messageSendDTO = JSONUtil.toBean(text, MessageSendDTO.class);
        channelContextUtil.sendMessage(messageSendDTO);
    }

    /**
     * 此处鉴权
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String token = handshakeComplete.requestHeaders().get("token");
            if(StrUtil.isEmpty(token)){
                String url = handshakeComplete.requestUri();
                token = getTokenFromUrl(url);
            }
            if(StrUtil.isEmpty(token)){
                ctx.writeAndFlush(new TextWebSocketFrame("token为null"));
                ctx.close();
            }
            //此处模拟用户1和2 ，3
            if(!Objects.equals(token, "1") && !Objects.equals(token, "2") && !Objects.equals(token, "3")){
                ctx.writeAndFlush(new TextWebSocketFrame("token为null"));
                ctx.close();
            }
            String userId = token;
            channelContextUtil.addContext(userId, ctx.channel());

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private String getTokenFromUrl(String url) {
        if (StrUtil.isEmpty(url)) {
            return null;
        }
        String[] split = url.split("\\?");
        if (split.length != 2) {
            return null;
        }
        Optional<String> token = Arrays.stream(split)
                .map(s -> s.split("="))
                .filter(params -> params.length == 2 && params[0].equals("token"))
                .map(params->params[1])
                .findFirst();
        return token.orElse(null);
    }


}
