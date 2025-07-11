package com.atguigu.daijia.map.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author guojiye
 * @description
 * @date 2025/6/2
 **/
@Configuration
public class MapConfig {

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
