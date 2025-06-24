package com.atguigu.daijia.dispatch.xxl.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

/**
 * @author guojiye
 * @description
 * @date 2025/6/4
 **/
@Component
public class DispatchJobHandler {

    @XxlJob("firstJobHandler")
    public void testJobHandler(){
        System.out.println("xxl-job 项目集成测试");
        String jobParam = XxlJobHelper.getJobParam();
        System.out.println("参数为：" + jobParam);
    }
}
