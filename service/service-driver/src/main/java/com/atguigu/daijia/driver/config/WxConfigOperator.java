package com.atguigu.daijia.driver.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author guojiye
 * @description
 * @date 2025/5/27
 **/
@Component
@RequiredArgsConstructor
public class WxConfigOperator {

    private final WxConfigProperties wxConfigProperties;


    /**
     * 初始化微信工具包需要的对象
     * @return WxMaService
     */
    @Bean
    public WxMaService wxMaService(){
        // 配置微信小程序相关信息
        WxMaService service = new WxMaServiceImpl();

        WxMaDefaultConfigImpl wxMaConfig = new WxMaDefaultConfigImpl();
        wxMaConfig.setAppid(wxConfigProperties.getAppId());
        wxMaConfig.setSecret(wxConfigProperties.getSecret());

        service.setWxMaConfig(wxMaConfig);

        return service;
    }
}
