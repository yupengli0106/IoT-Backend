package com.demo.myapp.service.impl;

import com.demo.myapp.handler.WebSocketHandler;
import com.demo.myapp.mapper.DeviceMapper;
import com.demo.myapp.mapper.EnergyMapper;
import com.demo.myapp.mapper.MqttSubscriptionMapper;
import com.demo.myapp.pojo.Energy;
import jakarta.annotation.Resource;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;


/**
 * @Author: Yupeng Li
 * @Date: 12/7/2024 15:05
 * @Description:
 */

@Service
public class MqttService {
    @Resource
    private IMqttAsyncClient mqttClient;
    @Resource
    private WebSocketHandler webSocketHandler;
    @Resource
    MqttSubscriptionMapper mqttSubscriptionMapper;
    @Resource
    private EnergyMapper energyMapper;
    @Resource
    private DeviceMapper deviceMapper;

    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);


    /**
     * 这里是为了在服务端重启的时候，通过查询数据库，重新关注所有之前已经存在关注的topic.
     * 在mqtt config中会通过application context来调用这个方法
     */
    public void resubscribeAllTopics() {
        try {
            List<String> topics = mqttSubscriptionMapper.findAllTopics();
            if (topics != null) {
                topics.forEach(this::subscribe);
            } else {
                logger.warn("No topics found to resubscribe.");
            }
        } catch (Exception e) {
            logger.error("Failed to find topics throw mqttSubscriptionMapper", e);
        }
        logger.info("Resubscribed to all topics in database successfully!");
    }

    /**
     * 发布消息
     * @param topic 主题
     * @param payload 消息内容
     */
    public void publish(String topic, String payload) {
        // TODO: could implement a message queue to store messages until the client reconnects?
        if (!mqttClient.isConnected()) {
            logger.error("MQTT client is not connected. Cannot publish to topic: {}", topic);
            return;
        }

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
        try {
            mqttClient.publish(topic, message, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    logger.info("Successfully published message to topic {}: {}", topic, payload);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    logger.error("Failed to publish message to topic {}: {}", topic, exception.getMessage());
                }
            });
        } catch (MqttException e) {
            logger.error("Exception while publishing message to topic {}: {}", topic, e.getMessage());
        }
    }

    /**
     * 订阅主题
     * @param topic 主题
     */
    public void subscribe(String topic) {
        if (!mqttClient.isConnected()) {
            logger.error("MQTT client is not connected. Cannot subscribe to topic: {}", topic);
            return;
        }

        try {
            mqttClient.subscribe(topic, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    logger.info("Subscribed to topic: {}", topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    logger.error("Failed to subscribe to topic {}: {}", topic, exception.getMessage());
                }
            }, (t, msg) -> { // handle incoming message here after successfully subscribed
                String payload = new String(msg.getPayload());
                handleIncomingData(payload);
            });
        } catch (MqttException e) {
            logger.error("Exception while subscribing to topic {}: {}", topic, e.getMessage());
        }
    }

    /**
     * 批量取消订阅
     * @param topics 主题列表
     * @throws MqttException 异常
     */
    public void unsubscribe(List<String> topics) throws MqttException {
        mqttClient.unsubscribe(topics.toArray(new String[0]), null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                logger.info("Unsubscribed from topics: {}", topics);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                logger.error("Failed to unsubscribe from topics: {}", topics);
            }
        });
    }

    /**
     * 处理接收到的设备数据，根据数据类型进行不同的处理。energy数据存储到数据库，其他数据通过WebSocket发送到前端展示。
     * @param data 数据内容
     */
    private void handleIncomingData(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            String sensorType = jsonObject.getString("sensorType");

            switch (sensorType) {
                case "energy":
                    handleEnergyData(jsonObject);
                    break;
                default:
                    handleSensorData(jsonObject);
                    break;
            }
        } catch (JSONException e) {
            logger.error("Failed to parse incoming data: {}", data, e);
        }
    }

    /**
     * private method to handle energy data, store to database, and for front-end display
     * @param jsonObject energy data in JSON format
     */
    private void handleEnergyData(JSONObject jsonObject) {
        try {
            long deviceId = jsonObject.getLong("deviceId");
            Date recordDate = Date.valueOf(jsonObject.getString("date"));
            BigDecimal totalEnergy = BigDecimal.valueOf(jsonObject.getDouble("totalEnergy"));
            String sensorType = jsonObject.getString("sensorType");

            Long userId = deviceMapper.findDeviceById(deviceId).getUserId();
            if (userId == null || userId <= 0) {
                logger.error("Failed to get user id by device id: {}", deviceId);
                return;
            }

            Energy energy = new Energy();
            energy.setDeviceId(deviceId);
            energy.setEnergy(totalEnergy);
            energy.setRecordDate(recordDate);
            energy.setUserId(userId);
            energy.setSensorType(sensorType);

            try {
                energyMapper.insertEnergy(energy);
                logger.info("Stored energy data successfully for deviceId: {}", deviceId);
            } catch (Exception e) {
                logger.error("Failed to store energy data for deviceId {}: {}", deviceId, e.getMessage());
            }
        } catch (JSONException e) {
            logger.error("Failed to parse energy data: {}", jsonObject.toString(), e);
        }
    }

    /**
     * private method to handle sensor data, send to clients via WebSocket to display
     * @param jsonObject sensor data in JSON format
     */
    private void handleSensorData(JSONObject jsonObject) {
        try {
            String data = jsonObject.toString();
            webSocketHandler.sendMessageToClients(data);
            logger.info("WebSocket successfully sent message to clients: {}", data);
        } catch (Exception e) {
            logger.error("Failed to send message to clients: {}", jsonObject.toString(), e);
        }
    }
}
