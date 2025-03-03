package com.mydemo.netty.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 石玉龙 at 2024/12/26 18:59
 */
@Component
@ConfigurationProperties(prefix = "netty")
@Data
public class NettyProperties {

    private Integer port;
}
