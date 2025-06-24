package com.atguigu.daijia.dispatch.xxl.client;

import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.xxl.config.XxlJobClientConfig;
import com.atguigu.daijia.dispatch.xxl.config.XxlJobConfig;
import com.atguigu.daijia.dispatch.xxl.config.XxlJobLoginHandler;
import com.atguigu.daijia.model.entity.dispatch.XxlJobInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpHead;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * @author guojiye
 * @description
 * @date 2025/6/4
 **/
@Component
@DependsOn("xxlJobLoginHandler")
@Slf4j
@RequiredArgsConstructor
public class XxlJobClient {

    private final XxlJobClientConfig xxlJobClientConfig;

    private final RestTemplate restTemplate;

    private final XxlJobLoginHandler xxlJobLoginHandler;


    @SneakyThrows
    public Long addJob(String executorHandler, String param, String cron, String desc) {
        log.info("开始新增XXL-JOB任务，executorHandler={}, cron={}", executorHandler, cron);

        int jobGroupId = xxlJobClientConfig.getJobGroupId();
        log.info("当前 jobGroup = {}", jobGroupId);
        if (jobGroupId <= 0) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        String cookie = xxlJobLoginHandler.getCookie();
        log.info("当前 Cookie = {}", cookie);

        // 构建表单数据
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("jobGroup", String.valueOf(jobGroupId));
        formData.add("jobDesc", desc);
        formData.add("author", "gjy");
        formData.add("scheduleType", "CRON");
        formData.add("scheduleConf", cron);
        formData.add("glueType", "BEAN");
        formData.add("executorHandler", executorHandler);
        formData.add("executorParam", param);
        formData.add("executorRouteStrategy", "FIRST");
        formData.add("executorBlockStrategy", "SERIAL_EXECUTION");
        formData.add("misfireStrategy", "FIRE_ONCE_NOW");
        formData.add("executorTimeout", "0");
        formData.add("executorFailRetryCount", "0");
        formData.add("glueRemark", "GLUE代码初始化");
        formData.add("childJobId", "");

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        if (cookie != null && !cookie.isEmpty()) {
            headers.set("Cookie", cookie);  // 添加 Cookie
        }

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

        RestTemplate restTemplate = new RestTemplate();

        String url = xxlJobClientConfig.getAddUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, requestEntity, JSONObject.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody().getIntValue("code") == 200) {
            log.info("增加xxl执行任务成功，返回信息：{}", response.getBody().toJSONString());
            return response.getBody().getLong("content");
        }

        log.warn("调用xxl增加执行任务失败：{}", response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }


    public Boolean startJob(Long jobId) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("id", jobId.toString());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.set("Cookie", xxlJobLoginHandler.getCookie());
        HttpEntity<MultiValueMap<String, String>> xxlJobInfoHttpEntity = new HttpEntity<>(formData, httpHeaders);

        String url = xxlJobClientConfig.getStartJobUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, xxlJobInfoHttpEntity, JSONObject.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody().getIntValue("code") == 200) {
            log.info("启动xxl执行任务成功，返回信息：{}", response.getBody().toJSONString());
            return true;
        }
        log.info("启动xxl执行任务失败：{}", response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }


    public Boolean stopJob(Long jobId) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("id", jobId.toString());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.set("Cookie", xxlJobLoginHandler.getCookie());
        HttpEntity<MultiValueMap<String, String>> xxlJobInfoHttpEntity = new HttpEntity<>(formData, httpHeaders);

        String url = xxlJobClientConfig.getStopJobUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, xxlJobInfoHttpEntity, JSONObject.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody().getIntValue("code") == 200) {
            log.info("停止xxl执行任务成功，返回信息：{}", response.getBody().toJSONString());
            return true;
        }
        log.info("停止xxl执行任务成功：{}", response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }

    public Boolean removeJob(Long jobId) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("id", jobId.toString());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.set("Cookie", xxlJobLoginHandler.getCookie());
        HttpEntity<MultiValueMap<String, String>> xxlJobInfoHttpEntity = new HttpEntity<>(formData, httpHeaders);

        String url = xxlJobClientConfig.getRemoveUrl();
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, xxlJobInfoHttpEntity, JSONObject.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody().getIntValue("code") == 200) {
            log.info("删除xxl执行任务成功，返回信息：{}", response.getBody().toJSONString());
            return true;
        }
        log.info("删除xxl执行任务失败：{}", response.getBody().toJSONString());
        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }


    // 添加并启动任务
    public Long addAndStart(String executorHandler, String param, String cron, String desc) {

        Long jobId = this.addJob(executorHandler, param, cron, desc);

        if (!this.startJob(jobId)) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        return jobId;
    }
}
