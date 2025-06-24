package com.atguigu.daijia.config;

import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author guojiye
 * @description
 * @date 2025/6/17
 **/
@Configuration
@ConfigurationProperties(prefix = "wx.v3pay")
@Data
public class WxPayV3Properties {

    private String appId;

    private String merchantId;

    private String privateKeyPath;

    private String merchantSerialNumber;

    private String apiV3key;

    private String notifyUrl;

    @Bean
    public RSAAutoCertificateConfig getConfig(){
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(this.getMerchantId())
                .privateKeyFromPath(this.getPrivateKeyPath())
                .merchantSerialNumber(this.getMerchantSerialNumber())
                .apiV3Key(this.getApiV3key())
                .build();
    }

}
