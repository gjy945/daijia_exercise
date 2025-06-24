package com.atguigu.daijia.handle;

import com.atguigu.daijia.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author guojiye
 * @description 监听延迟队列
 * @date 2025/6/18
 **/
@Component
@RequiredArgsConstructor
public class RedisDelayHandle {

    private final RedissonClient redissonClient;

    private final OrderInfoService orderInfoService;

    @PostConstruct
    public void listener() {
        new Thread(() -> {
            while (true){
                // 获取到延迟队列中的阻塞队列
                RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue("queue_cancel");

                try {
                    // 从队列获取消息
                    String orderId = blockingQueue.take();

                    // 取消订单
                    if (StringUtils.hasText(orderId)){
                        // 调用方法取消订单
                        orderInfoService.orderCancel(Long.parseLong(orderId));
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

}
