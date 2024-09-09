package com.demo.myapp.service.impl;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.mapper.DeviceMapper;
import com.demo.myapp.mapper.EnergyMapper;
import com.demo.myapp.mapper.MqttSubscriptionMapper;
import com.demo.myapp.pojo.Device;
import com.demo.myapp.pojo.Energy;
import com.demo.myapp.pojo.LoginUser;
import com.demo.myapp.pojo.MqttSubscription;
import com.demo.myapp.service.DeviceService;
import com.demo.myapp.service.UserActivityService;
import jakarta.annotation.Resource;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: Yupeng Li
 * @Date: 11/7/2024 23:20
 * @Description:
 */
@Service
public class DeviceServiceImpl implements DeviceService {
    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private EnergyMapper energyMapper;

    @Resource
    private MqttService mqttService;

    @Resource
    private UserService userService;

    @Resource
    private UserActivityService userActivityService;

    @Resource
    private MqttSubscriptionMapper mqttSubscriptionMapper;

    @Override
    public List<Device> getAllDevices() {
        return deviceMapper.findAllDevices();
    }

    @Override
    public Device getDeviceById(Long id) {
        return deviceMapper.findDeviceById(id);
    }

    @Override
    @Transactional
    public ResponseEntity<Result> addDevice(Device device) {
        // 获取当前登录用户的ID
        Long userId = userService.getCurrentUserId();
        // 检查设备名称是否重复
        if (deviceMapper.findDeviceByName(device.getName(),userId) != null) {
            return ResponseEntity.badRequest().body(Result.error(405, "Device name already exists"));
        }

        device.setUserId(userId);
        device.setStatus("OFF"); // set default status
        device.setName(device.getName());
        try { // 插入设备记录到数据库
            deviceMapper.insertDevice(device);

            //记录用户操作
            userActivityService.logUserActivity(
                    userId,
                    userService.getCurrentUsername(),
                    device.getName(),
                    "Add device by user '" + userService.getCurrentUsername() + "', default status is 'OFF', at time " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );


            //TODO: 这里需要配合一个写死的模拟器来使用，后续可以改进

            // 添加设备时发布设备初始状态，以保持一致性
            String statusTopic = "home/device/" + device.getId() + "/status";
            mqttService.publish(statusTopic, "OFF");

            // 订阅设备端的publish主题，可以接收到设备端传过来的数据
            mqttService.subscribe("home/device/" + device.getId() + "/data");

            // 添加订阅的主题到数据库
            MqttSubscription mqttSubscription = new MqttSubscription();
            mqttSubscription.setTopic("home/device/" + device.getId() + "/data");
            mqttSubscription.setUserId(userId);
            mqttSubscriptionMapper.insert(mqttSubscription);

            return ResponseEntity.ok(Result.success("Device added successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Result.error(405, "Error adding device"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Result> updateDevice(Long id, Device device) {
        Long userId = userService.getCurrentUserId();
        // 检查设备名称是否重复
        if (deviceMapper.findDeviceByName(device.getName(), userId) != null) {
            return ResponseEntity.badRequest().body(Result.error(405, "Device name already exists"));
        }
        device.setUserId(userId);
        device.setId(id);
        device.setType(device.getType());
        device.setName(device.getName());
        device.setStatus(device.getStatus());

        try { // 更新设备记录到数据库
            deviceMapper.editDevice(device);
            //记录用户操作
            userActivityService.logUserActivity(
                    userId,
                    userService.getCurrentUsername(),
                    device.getName(),
                    "Update device by user '" +
                            userService.getCurrentUsername()
                            + "', device name is '" + device.getName()
                            + "', device type is '" + device.getType()
                            + "' at time " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            return ResponseEntity.ok(Result.success("Device updated successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Result.error(405, "Error updating device"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Result> deleteDevice(List<Long> ids) {
        Long userId = userService.getCurrentUserId();
        try {
            for (Long id : ids) {
                deviceMapper.deleteDeviceById(id, userId); // 删除设备
                //记录用户操作
                userActivityService.logUserActivity(
                        userId,
                        userService.getCurrentUsername(),
                        id.toString(),
                        "Delete device by user '" +
                                userService.getCurrentUsername() +
                                "', device id is '" + id +
                                "', at time " +
                                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                );
                mqttService.unsubscribe("home/device/" + id + "/data"); // 取消订阅
                mqttSubscriptionMapper.delete("home/device/" + id + "/data", userId); // 删除在数据库中的订阅记录
            }
            return ResponseEntity.ok(Result.success("Device deleted successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Result.error(405, "Error deleting device"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Result> controlDevice(Long id, String command) {
        command = command.toUpperCase(); // 转换为大写
        Long userId = userService.getCurrentUserId();
        Device device = deviceMapper.findDeviceById(id);
        if (device != null) {
            device.setUserId(userId);
            device.setStatus(command);
            try { // 使用MQTT发送控制命令
                String topic = "home/device/" + device.getId() + "/status";
                mqttService.publish(topic, command);// 发布消息
                deviceMapper.updateDeviceStatus(device); // 更新设备状态在数据库
                //记录用户操作
                userActivityService.logUserActivity(
                        userId,
                        userService.getCurrentUsername(),
                        device.getName(),
                        "Controlled device by user '" +
                                userService.getCurrentUsername() +
                                "', device name is '" + device.getName() +
                                "', command is '" + command +
                                "', at time " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                );
                return ResponseEntity.ok(Result.success("Device controlled successfully"));
            } catch (MqttException e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().body(Result.error(405, "Error controlling device"));
            }
        }
        return ResponseEntity.badRequest().body(Result.error(405, "Device not found"));
    }

    @Override
    public Page<Device> getDevicesByPage(Pageable pageable) { // TODO: 后续可以改进，将数据存在redis中，提高查询速度？
        //get current user id
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        long currentUserId = loginUser.getUser().getId();
        // 分页查询设备
        int pageSize = pageable.getPageSize();
        int offset = pageable.getPageNumber() * pageSize;
        long totalDevices = deviceMapper.countDevices(currentUserId); // 计算设备的总数
        if (offset >= totalDevices) { // 如果偏移量大于等于设备总数，返回空列表
            return new PageImpl<>(List.of(), pageable, 0);
        }
        List<Device> devices = deviceMapper.findDevicesByPage(currentUserId,pageSize, offset);
        return new PageImpl<>(devices, pageable, totalDevices);
    }

    @Override
    public long countDevices() {
        return deviceMapper.countDevices(userService.getCurrentUserId());
    }

    @Override
    public long getOnlineDevices() {
        return deviceMapper.countOnlineDevices(userService.getCurrentUserId());
    }

    @Override
    public long getOfflineDevices() {
        return deviceMapper.countOfflineDevices(userService.getCurrentUserId());
    }

    @Override
    public List<Energy> getAllEnergy() {
        Long userId = userService.getCurrentUserId();
        return energyMapper.getAllEnergy(userId);
    }
}
