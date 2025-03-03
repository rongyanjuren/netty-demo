package com.mydemo.netty.spring;

import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@ChannelHandler.Sharable
//public class MyServerHandler extends ChannelInboundHandlerAdapter {
public class MyIdleStateHandler extends ChannelDuplexHandler {



    /**
     * 当 IdleStateEvent 触发后 , 就会传递给管道 的下一个handler去处理，通过调用(触发)
     * 下一个handler 的 userEventTiggered , 在该方法中去处理 IdleStateEvent(读空闲，写空闲，读写空闲)
     *
     * @param ctx 上下文
     * @param evt 事件
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof IdleStateEvent) {

            //将  evt 向下转型 IdleStateEvent
            IdleStateEvent event = (IdleStateEvent) evt;
            String eventType = null;
            switch (event.state()) {
                case READER_IDLE:
                    eventType = "读空闲";
                    break;
                case WRITER_IDLE:
                    eventType = "写空闲";
                    break;
                case ALL_IDLE:
                    eventType = "读写空闲";
                    break;
            }
            Channel channel = ctx.channel();
            Attribute<String> attr = channel.attr(AttributeKey.valueOf(channel.id().toString()));
            String userId = attr.get();
            log.info("超时的userId:{},事件:{}", userId,eventType);
            //发送消息，如果失败就关闭
            channel.writeAndFlush(new TextWebSocketFrame("超时后发给客户端的消息")).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

        }
    }
}

