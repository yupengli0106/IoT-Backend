package com.demo.myapp.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @Author: Yupeng Li
 * @Date: 11/7/2024 23:30
 * @Description: TODO: 后续可以考虑使用更安全的链接方式，例如SSL
 */

@Configuration
public class MqttConfig {
    @Value("${mqtt.broker}")
    private String broker;

    @Value("${mqtt.clientId}")
    private String clientId;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Bean
    public MqttClient mqttClient(){
        final Logger logger = LoggerFactory.getLogger(MqttConfig.class);

        MqttClient client = null;
        try {
            client = new MqttClient(broker, clientId, new MemoryPersistence());

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(false); // 设置为false以启用持久会话, 不会因为客户端断开而取消原有的订阅。
            connOpts.setAutomaticReconnect(true); // 设置为true以启用自动重连
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            //开始连接
            client.connect(connOpts);

            logger.info("Connected to MQTT broker successfully");
        } catch (MqttException e) {
            e.printStackTrace();
            logger.error("Mqtt client connection failed: {}", e.getMessage());
        }

        return client;
    }
}
