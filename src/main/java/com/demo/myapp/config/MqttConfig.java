package com.demo.myapp.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

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
            /*
            使用项目根目录下的文件持久化, 用于保存订阅信息.
            这样即使客户端断开连接, 或者服务重启, 也不会丢失订阅信息。
             */
            String persistenceDir = "./mqtt_persistence";
            File dir = new File(persistenceDir);
            if (!dir.exists()) {
                try {
                    dir.mkdirs(); // 创建目录
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("Failed to create persistence directory: {}", e.getMessage());
                }
                logger.info("Created persistence directory: {}", dir.getAbsolutePath());
            } else {
                logger.info("Using existing persistence directory: {}", dir.getAbsolutePath());
            }

            MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence(persistenceDir);

            client = new MqttClient(broker, clientId, persistence);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(false); // 设置为false以启用持久会话, 不会因为客户端断开而取消原有的订阅。
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());

            client.connect(connOpts);
            logger.info("Connected to MQTT broker successfully");
        } catch (MqttException e) {
            e.printStackTrace();
            logger.error("Mqtt client connection failed: {}", e.getMessage());
        }

        return client;
    }
}
