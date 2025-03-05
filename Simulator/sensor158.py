import paho.mqtt.client as mqtt
import time
import random
import json
from colorama import init, Fore, Style
from datetime import datetime

# colorama 给终端输出添加颜色
init(autoreset=True)

# MQTT broker configuration
broker = "120.25.104.84"  # broker IP address
port = 1883
username = "root"  # username if required
password = "Sqysb0118."  # password if required
publish_topic = "home/device/158/data"  # 用来发布数据的主题
subscribe_topic = "home/device/158/status"  # 用户接受命令的主题

# 设备状态，初始化为 OFF
device_status = "OFF"
total_energy_consumed = 0.0

# The callback for when the client receives a CONNACK response from the server.
def on_connect(client, userdata, flags, reason_code, properties):
    print(f"Connected with result code {reason_code}")
    client.subscribe(subscribe_topic)
    print(Fore.CYAN + f"Subscribed to topic: {subscribe_topic}")

# The callback for when a PUBLISH message is received from the server.
def on_message(client, userdata, msg):
    global device_status
    command = msg.payload.decode().lower()
    print(Fore.YELLOW + f"Received command: {command}")
    if command == "on":
        device_status = "ON"
        print(Fore.GREEN + "Device turned ON")
    elif command == "off":
        device_status = "OFF"
        print(Fore.RED + "Device turned OFF")
    else:
        print(Fore.RED + "Unknown command")


# Function to calculate energy consumption based on sensor data
def calculate_energy(sensor_value):
    # 假设能耗与传感器值成正比
    return sensor_value * 0.1  # 示例计算，每个单位值消耗 0.1 能量


# 创建 MQTT 客户端
client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
client.on_connect = on_connect
client.on_message = on_message
client.username_pw_set(username, password)

try:
    client.connect(broker, port, 60)
except Exception as e:
    print(Fore.RED + f"Error connecting to broker: {e}")
    exit(1)

client.loop_start()

try:
    # 当前日期，用于追踪当天能耗
    current_day = datetime.now().date()

    while True:
        if device_status == "ON":
            # 生成随机温度和湿度数据
            temperature = random.uniform(20.0, 30.0)
            humidity = random.uniform(30.0, 70.0)

            # 根据温度和湿度数据计算能耗
            energy_from_temperature = calculate_energy(temperature)
            energy_from_humidity = calculate_energy(humidity)
            total_energy_consumed += energy_from_temperature + energy_from_humidity

            # 创建并发送温度数据
            temperature_data = {
                "sensorType": "temperature",
                "sensorValue": round(temperature, 2)
            }
            client.publish(publish_topic, json.dumps(temperature_data))
            print(Fore.CYAN + f"Published temperature data: {json.dumps(temperature_data)}")

            time.sleep(1)  # 等待1秒后发送湿度数据

            # 创建并发送湿度数据
            humidity_data = {
                "sensorType": "humidity",
                "sensorValue": round(humidity, 2)
            }
            client.publish(publish_topic, json.dumps(humidity_data))
            print(Fore.CYAN + f"Published humidity data: {json.dumps(humidity_data)}")

        else:
            # 当设备关闭时不发送数据
            time.sleep(5)  # 避免持续输出，适当等待一段时间
            print(Fore.LIGHTBLACK_EX + "Device is OFF, not sending data.")

        time.sleep(5)  # 每5秒发送一次数据

        # 检查是否新的一天开始
        now = datetime.now().date()
        if now != current_day:
            # 发布当天的总能耗数据
            energy_data = {
                "sensorType": "energy",
                "deviceId": 158,
                "date": str(current_day),
                "totalEnergy": round(total_energy_consumed, 2)
            }
            client.publish(publish_topic, json.dumps(energy_data))
            print(Fore.MAGENTA + f"Published daily energy data: {json.dumps(energy_data)}")

            # 重置能耗并更新日期
            total_energy_consumed = 0.0
            current_day = now
            print(Fore.MAGENTA + f"Reset total energy consumed for the new day.")

except KeyboardInterrupt:
    print(Style.BRIGHT + "\nStopping simulator.")
finally:
    client.loop_stop()
    client.disconnect()
    print(Fore.MAGENTA + "Disconnected from broker.")






