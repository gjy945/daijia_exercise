package com.atguigu.daijia.customer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author guojiye
 * @description
 * @date 2025/5/27
 **/
@Component
@Data
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxConfigProperties {

    private String appId;

    private String secret;

    // 微信默认头像
    public static final String defaultAvatar = "https://ss0.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=2279408239,3825398873&fm=253&gp=0.jpg";
}
