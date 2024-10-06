package com.demo.myapp.config;

import com.demo.myapp.service.impl.MqttService;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @Author: Yupeng Li
 * @Date: 11/7/2024 23:30
 * @Description: TODO: 后续可以考虑使用更安全的链接方式，例如SSL
 */

@Configuration
public class MqttConfig implements ApplicationContextAware {
    @Value("${mqtt.broker}")
    private String broker;

    @Value("${mqtt.clientId}")
    private String clientId;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    private ApplicationContext applicationContext;

    @Bean
    public MqttAsyncClient mqttClient(){
        final Logger logger = LoggerFactory.getLogger(MqttConfig.class);

        MqttAsyncClient client = null;
        try {
            // 使用异步客户端
            client = new MqttAsyncClient(broker, clientId, new MemoryPersistence());

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(false); // 设置为false以启用持久会话, 不会因为客户端断开而取消原有的订阅。
            connOpts.setAutomaticReconnect(true); // 设置为true以启用自动重连
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            connOpts.getKeepAliveInterval(); // 默认为60s
            // 设置回调函数
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    // 连接成功后，重新订阅所有topic
                    // TODO: 这里是每次有新的连接时都会重新订阅所有topic，后续可以考虑优化？阻塞client.connect(connOpts),然后用postConstruct来订阅所有topic？
                    logger.info("Connected to MQTT broker at {}", serverURI);
                    if (reconnect) {
                        logger.info("Reconnected to MQTT broker, resubscribing to topics.");
                    }
                    // Resubscribe to topics
                    ApplicationContext ctx = applicationContext;
                    if (applicationContext == null) {
                        logger.error("ApplicationContext is not initialized. Cannot resubscribe to topics.");
                        return;
                    }
                    MqttService mqttService = ctx.getBean(MqttService.class);
                    mqttService.resubscribeAllTopics();
                    logger.info("Resubscribed to all topics successfully!");
                }

                @Override
                public void connectionLost(Throwable throwable) {
                    logger.error("Connection to MQTT broker lost: {}", throwable.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // This can be left empty as we're using individual message listeners at the subscription level
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //Handle delivery confirmation
                }
            });

            //阻塞直到连接成功
            client.connect(connOpts);
            logger.info("Connected to MQTT broker successfully");

        } catch (MqttException e) {
            logger.error("Mqtt client connection failed: {}", e.getMessage());
        }

        return client;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
