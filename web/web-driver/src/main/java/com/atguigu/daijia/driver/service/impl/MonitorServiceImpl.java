package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.client.CiFeignClient;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.driver.service.MonitorService;
import com.atguigu.daijia.model.entity.order.OrderMonitor;
import com.atguigu.daijia.model.entity.order.OrderMonitorRecord;
import com.atguigu.daijia.model.form.order.OrderMonitorForm;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import com.atguigu.daijia.model.vo.order.TextAuditingVo;
import com.atguigu.daijia.order.client.OrderMonitorFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MonitorServiceImpl implements MonitorService {

    private final OrderMonitorFeignClient orderMonitorFeignClient;

    private final CosService cosService;

    private final CiFeignClient ciFeignClient;

    @Override
    public Boolean upload(MultipartFile file, OrderMonitorForm orderForm) {
        // 上传文件
        CosUploadVo cosUploadVo = cosService.uploadFile(file, "recording");
        String showUrl = cosUploadVo.getShowUrl();

        OrderMonitorRecord orderMonitorRecord = new OrderMonitorRecord();
        orderMonitorRecord.setOrderId(orderForm.getOrderId());
        orderMonitorRecord.setFileUrl(showUrl);
        orderMonitorRecord.setContent(orderForm.getContent());

        // 增加文本内容的审核
        Result<TextAuditingVo> textAuditingVoResult = ciFeignClient.textAuditing(orderForm.getContent());
        TextAuditingVo textAuditingVo = textAuditingVoResult.getData();

        orderMonitorRecord.setResult(textAuditingVo.getResult());
        orderMonitorRecord.setKeywords(textAuditingVo.getKeywords());

        orderMonitorFeignClient.saveOrderMonitorRecord(orderMonitorRecord);

        // 更新订单控制统计
        OrderMonitor data = orderMonitorFeignClient.getOrderMonitor(orderForm.getOrderId()).getData();
        int fileNum = data.getFileNum() + 1;
        data.setFileNum(fileNum);
        // 审核结果 0正常 1违规敏感文件 2疑似违规文件
        if ("3".equals(orderMonitorRecord.getResult())){
            int i = data.getAuditNum() + 1;
            data.setAuditNum(i);
        }

        return orderMonitorFeignClient.updateOrderMonitor(data).getData();
    }
}
