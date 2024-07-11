package com.demo.myapp.service.impl;

import com.demo.myapp.config.RabbitMQConfig;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * @Author: Yupeng Li
 * @Date: 11/7/2024 23:26
 * @Description:
 */
@Service
public class RabbitMQProducerService {
    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_NAME, message);
    }
}
