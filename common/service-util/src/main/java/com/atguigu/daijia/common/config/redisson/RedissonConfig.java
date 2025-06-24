package com.atguigu.daijia.common.config.redisson;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author guojiye
 * @description redisson配置
 * @date 2025/6/9
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedissonConfig {

    private String host;

    private String password;

    private String port;

    private int timeout = 3000;

    private static String ADDRESS_PREFIX = "redis://";

    @Bean
    RedissonClient redissonClient(){
        Config config = new Config();

        if (!StringUtils.hasText(host)){
            throw new RuntimeException("host is empty");
        }
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(ADDRESS_PREFIX + this.host + ":" + port)
                .setTimeout(this.timeout);
        if (StringUtils.hasText(this.password)){
            singleServerConfig.setPassword(this.password);
        }
        return Redisson.create(config);
    }

}
