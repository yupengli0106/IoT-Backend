package com.demo.myapp.service.impl;

import jakarta.annotation.Resource;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @Author: Yupeng Li
 * @Date: 12/7/2024 15:05
 * @Description:
 */

@Service
public class MqttService {

    @Resource
    private IMqttClient mqttClient;

    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);

    /**
     * 发布消息
     * @param topic 主题
     * @param payload 消息内容
     * @throws MqttException 异常
     */
    public void publish(String topic, String payload) throws MqttException {
        MqttMessage message = new MqttMessage(payload.getBytes());
        /*
         * QoS 级别简述:
         * 	1.	QoS 0 - 最多一次 (At most once)
         * 	•	解释：消息最多传递一次，不保证消息到达。
         * 	2.	QoS 1 - 至少一次 (At least once)
         * 	•	解释：消息至少传递一次，确保消息到达，但可能重复。
         * 	3.	QoS 2 - 只有一次 (Exactly once)
         * 	•	解释：消息仅传递一次，确保不重复也不丢失
         */
        message.setQos(1);
        mqttClient.publish(topic, message);
        logger.info("Published message to topic {}: {}", topic, payload);
    }

    /**
     * 订阅主题
     * @param topic 主题
     * @throws MqttException 异常
     */
    public void subscribe(String topic) throws MqttException {
        mqttClient.subscribe(topic, (t, msg) -> {
            String payload = new String(msg.getPayload());
            logger.info("Received message on topic {}: {}", t, payload);
            System.out.println("Received message: " + payload);
            // 处理接收到的消息
            handleIncomingData(payload);
        });
        logger.info("Subscribed to topic: {}", topic);
    }

    /**
     * 取消订阅
     * @param topic 主题
     * @throws MqttException 异常
     */
    public void unsubscribe(String topic) throws MqttException {
        mqttClient.unsubscribe(topic);
        logger.info("Unsubscribed from topic: {}", topic);
    }

    /**
     * 处理接收到的设备数据
     * @param data 数据内容
     */
    private void handleIncomingData(String data) {
        // 在这里处理从设备接收到的数据
        System.out.println("Processed data: " + data);
        // 可以将数据存储到数据库、更新UI、或触发其他操作
    }
}
