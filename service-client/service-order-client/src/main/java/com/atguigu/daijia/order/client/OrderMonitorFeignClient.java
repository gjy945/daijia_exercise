package com.atguigu.daijia.order.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.order.OrderMonitor;
import com.atguigu.daijia.model.entity.order.OrderMonitorRecord;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(value = "service-order")
public interface OrderMonitorFeignClient {

    @PostMapping("/order/monitor/saveOrderMonitorRecord")
    Result<Boolean> saveOrderMonitorRecord(@RequestBody OrderMonitorRecord orderMonitorRecord);

    @GetMapping("/order/monitor/getOrderMonitor/{orderId}")
    Result<OrderMonitor> getOrderMonitor(@PathVariable Long orderId);

    @PostMapping("/order/monitor/updateOrderMonitor")
    Result<Boolean> updateOrderMonitor(@RequestBody OrderMonitor orderMonitor);
}