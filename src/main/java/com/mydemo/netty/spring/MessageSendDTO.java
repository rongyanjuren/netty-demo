package com.mydemo.netty.spring;

import lombok.Data;

/**
 * @author 石玉龙 at 2024/12/26 18:32
 */
@Data
public class MessageSendDTO {

    /**
     * 消息
     */
    private String msg;

    /**
     * 发送人
     */
    private String from;

    /**
     * 接收人
     */
    private String to;

    /**
     * 类型，1=个人 2=组
     */
    private Integer type;



}
