package com.mydemo.netty.spring;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 石玉龙 at 2024/12/26 14:23
 */
@Slf4j
@Component
public class ChannelContextUtil {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 用户和通道
     */
    private static final Map<String, Channel> USER_CHANNEL_MAP = new ConcurrentHashMap<>();
    /**
     * 组和组通道
     */
    private static final Map<String, ChannelGroup> GROUP_MAP = new ConcurrentHashMap<>();

    private static final String MESSAGE_TOPIC = "message";
    /**
     * 模拟GROUP_ID
     */
    private static final String GROUP_ID = "123";

    @PostConstruct
    public void lisMessage() {
        RTopic topic = redissonClient.getTopic(MESSAGE_TOPIC);
        topic.addListener(MessageSendDTO.class, (MessageSendDTO, messageSendDTO) -> {
            String jsonStr = JSONUtil.toJsonStr(messageSendDTO);
            log.info("收到广播消息:{}", jsonStr);
            //根据消息类型判断是发给个人还是群组
            //1=个人 2=组
            if (messageSendDTO.getType() == 1) {
                send2User(messageSendDTO.getTo(), jsonStr);
            } else if (messageSendDTO.getType() == 2) {
                send2Group(GROUP_ID, jsonStr);
            }
        });
    }


    public void sendMessage(MessageSendDTO message) {
        RTopic topic = redissonClient.getTopic(MESSAGE_TOPIC);
        topic.publish(message);
    }

    public void addContext(String userId, Channel channel) {
        String channelId = channel.id().toString();
        log.info("channelId:{}", channelId);
        AttributeKey<String> attributeKey = null;
        //不存在则创建，存在则获取
        if (!AttributeKey.exists(channelId)) {
            attributeKey = AttributeKey.newInstance(channelId);
        } else {
            attributeKey = AttributeKey.valueOf(channelId);
        }
        channel.attr(attributeKey).set(userId);
        USER_CHANNEL_MAP.put(userId, channel);
        //此处模拟用户1和用户2为1组,组id为123
        if (userId.equals("1") || userId.equals("2")) {
            add2Group(GROUP_ID, channel);
        }
    }

    //连接的时候加入组
    private void add2Group(String groupId, Channel channel) {
        ChannelGroup channelGroup = GROUP_MAP.getOrDefault(groupId, new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
        channelGroup.add(channel);
        GROUP_MAP.put(groupId, channelGroup);
    }


    private void send2Group(String groupId, String message) {
        ChannelGroup group = GROUP_MAP.get(groupId);
        if (group == null) {
            return;
        }
        //后端判断是否发给自己
//        group.forEach(ch -> {
//            if (channel != ch) { //不是当前的channel,转发消息
//                ch.writeAndFlush("[客户]" + channel.remoteAddress() + " 发送了消息" + msg + "\n");
//            } else {//回显自己发送的消息给自己
//                ch.writeAndFlush("[自己]发送了消息" + msg + "\n");
//            }
//        });
        //群里发消息给所有人，包括自己，前端判断是否显示
        group.writeAndFlush(new TextWebSocketFrame(message));
    }

    /**
     * 发送消息给用户
     *
     * @param receiveId 消息接收人
     * @param message   发送的消息，string类型
     */
    private void send2User(String receiveId, String message) {
        Channel channel = USER_CHANNEL_MAP.get(receiveId);
        if (channel == null) {
            return;
        }
        channel.writeAndFlush(new TextWebSocketFrame(message));
    }

    public void removeUser(Channel channel) {
        Attribute<String> attr = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        String userId = attr.get();
        if (StrUtil.isEmpty(userId)) {
            return;
        }
        log.info("userId:{}断开了连接", userId);
        USER_CHANNEL_MAP.remove(userId);
        //获取用户的组群
        ChannelGroup channels = GROUP_MAP.get(GROUP_ID);
        if (channels != null) {
            channels.remove(channel);
            if (channels.isEmpty()) {
                GROUP_MAP.remove(GROUP_ID);
            }
        }

    }
}
