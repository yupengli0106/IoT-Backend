package com.demo.myapp.service.impl;

import com.demo.myapp.config.RabbitMQConfig;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * @Author: Yupeng Li
 * @Date: 11/7/2024 23:27
 * @Description:
 */
@Service
public class RabbitMQConsumerService {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receiveMessage(String message) {
        System.out.println("Received message: " + message);
        // 处理控制指令
    }
}
