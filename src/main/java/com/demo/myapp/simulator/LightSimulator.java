package com.demo.myapp.simulator;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * @Author: Yupeng Li
 * @Date: 12/7/2024 14:59
 * @Description: LightSimulator 模拟了一个现实中的灯。
 * 这个模拟器订阅了一个 MQTT 主题，用于接收控制命令（例如 “on” 和 “off”），并在控制台上打印相应的状态信息。
 * 这样，它就像一个物理灯一样，可以通过 MQTT 消息进行控制。
 *
 * 设备模拟器工作流程
 * 	1.	连接到 MQTT 代理：
 * 模拟器首先连接到 Mosquitto 代理。
 * (确保本地已经安装了 Mosquitto 代理broker, 并且已经启动. brew install mosquitto, brew services start mosquitto for mac)
 * 	2.	订阅控制命令主题：
 * 模拟器订阅一个特定的 MQTT 主题（例如 “home/light/1/control”）。这个主题用于接收控制命令。
 * 	3.	处理接收到的控制命令：
 * 当模拟器接收到控制命令时，它会根据命令的内容（例如 “on” 或 “off”）在控制台上打印相应的状态信息。
 */


public class LightSimulator {

    private static final String BROKER = "tcp://localhost:1883"; // Mosquitto服务地址
    private static final String CLIENT_ID = MqttClient.generateClientId();
    private static final String DEVICE_TYPE = "Light"; // 设备类型(需要与数据库中的设备类型保持一致)
    private static final Long DEVICE_ID = 1L; // 设备ID（需要与数据库中的设备ID保持一致）
    private static final String CONTROL_TOPIC = "home/" + DEVICE_TYPE + "/" + DEVICE_ID + "/control";


    public static void main(String[] args) {
        try {
            IMqttClient client = new MqttClient(BROKER, CLIENT_ID, new MemoryPersistence());

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: " + BROKER);

            client.connect(connOpts);
            System.out.println("Connected successfully\n");

            System.out.println("Subscribing to topic: " + CONTROL_TOPIC);
            client.subscribe(CONTROL_TOPIC, (topic, message) -> {
                String payload = new String(message.getPayload());
                System.out.println("\nReceived control command: " + payload);
                if ("on".equalsIgnoreCase(payload)) {
                    System.out.println("Light is ON");
                } else if ("off".equalsIgnoreCase(payload)) {
                    System.out.println("Light is OFF");
                }
            });
            System.out.println("Subscribed successfully\n");

            System.out.println("Device simulator is running");
            while (true) {
                // 保持客户端运行
                Thread.sleep(10000);
            }

        } catch (MqttException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
