package com.mydemo.netty.spring;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 石玉龙 at 2024/12/23 14:08
 */
@Component
public class NettyServerHandlerInitializer extends ChannelInitializer<SocketChannel> {

    @Autowired
    private MyHttpServerHandler myHttpServerHandler;
    @Autowired
    private MyIdleStateHandler myIdleStateHandler;


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
                //因为基于http协议，使用http的编码和解码器
                .addLast(new HttpServerCodec())
                //http是以块方式写，添加ChunkedWriteHandler处理器
                .addLast(new ChunkedWriteHandler())
                /*
                说明
                1. http数据在传输过程中是分段, HttpObjectAggregator ，就是可以将多个段聚合
                2. 这就就是为什么，当浏览器发送大量数据时，就会发出多次http请求
                 */
                .addLast(new HttpObjectAggregator(1024 * 64))
                //空闲检测
                .addLast(new IdleStateHandler(5, 0, 0))
                .addLast(myIdleStateHandler)

                /*
                说明
                1. 对应websocket ，它的数据是以 帧(frame) 形式传递
                2. 可以看到WebSocketFrame 下面有六个子类
                3. 浏览器请求时 ws://localhost:7000/hello 表示请求的uri
                4. WebSocketServerProtocolHandler 核心功能是将 http协议升级为 ws协议 , 保持长连接
                5. 是通过一个 状态码 101
                 */
                .addLast(new WebSocketServerProtocolHandler("/hello2", null, true, 64 * 1024, true, true, 1000L))
                .addLast(myHttpServerHandler);
    }
}