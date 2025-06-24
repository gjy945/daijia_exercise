package com.atguigu.daijia.dispatch.xxl.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

@Component
@DependsOn("xxlJobConfig")
public class XxlJobLoginHandler {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private XxlJobConfig xxlJobConfig;

    private String cookie = "";

    public String getCookie() {
        return cookie;
    }

    @PostConstruct
    public void loginToXxlJobAdmin() {
        String loginUrl = xxlJobConfig.getAdminAddresses() + "/login";

        // 构造登录参数
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("userName", "admin");
        formData.add("password", "123456"); // e10adc3949ba59abbe56e057f20f883e MD5

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.postForEntity(loginUrl, request, String.class);

        // 提取 Cookie
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies != null && !cookies.isEmpty()) {
            this.cookie = cookies.get(0); // 保存 Cookie
            System.out.println("✅ 登录成功，Cookie 已保存：" + this.cookie);
        } else {
            System.err.println("❌ 登录失败，未获取到 Cookie");
        }
    }
}