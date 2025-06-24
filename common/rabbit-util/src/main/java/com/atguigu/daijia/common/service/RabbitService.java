package com.atguigu.daijia.common.service;


import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RabbitService {

    private final RabbitTemplate rabbitTemplate;

    // 发送消息
    public boolean sendMessage(String exchange,
                               String routingKey,
                               Object message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        return true;
    }

}
