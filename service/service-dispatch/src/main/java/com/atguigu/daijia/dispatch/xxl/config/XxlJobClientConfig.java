package com.atguigu.daijia.dispatch.xxl.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author guojiye
 * @description
 * @date 2025/6/4
 **/
@Data
@Component
@ConfigurationProperties(prefix = "xxl.job.client")
public class XxlJobClientConfig {

    private Integer jobGroupId;
    private String addUrl;
    private String startJobUrl;
    private String stopJobUrl;
    private String addAndStartUrl;
    private String removeUrl;
    private String loginUrl;
}
