package com.demo.myapp.service.impl;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.mapper.DeviceMapper;
import com.demo.myapp.mapper.EnergyMapper;
import com.demo.myapp.mapper.MqttSubscriptionMapper;
import com.demo.myapp.pojo.*;
import com.demo.myapp.service.DeviceService;
import com.demo.myapp.service.UserActivityService;
import jakarta.annotation.Resource;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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

    private static final Logger logger = LoggerFactory.getLogger(DeviceServiceImpl.class);
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @Override
    public List<Device> getAllDevices() {
        // TODO: 缓存设备到redis中，提高查询速度。在删除或更新设备时，清除相应的缓存。
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
            return ResponseEntity.badRequest().body(Result.error(409, "Device name already exists"));
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
            initializeDeviceMqttSettings(device);

            return ResponseEntity.ok(Result.success("Device added successfully"));
        } catch (Exception e) {
            logger.error("Error adding device", e);
            //TODO: 这里是不是不能吞掉异常，需要抛出异常，然后全局异常处理？不然会导致事物无法回滚？
            //throw new DeviceOperationException("Error adding device", e);
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
            logger.error("Error updating device", e);
            //TODO: 这里是不是不能吞掉异常，需要抛出异常，然后全局异常处理？不然会导致事物无法回滚？
            return ResponseEntity.badRequest().body(Result.error(405, "Error updating device"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Result> deleteDevice(List<Long> ids) {
        Long userId = userService.getCurrentUserId();
        String username = userService.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();
        String timestamp = DATE_TIME_FORMATTER.format(now);

        try {
            // 批量删除设备
            // TODO：这里还需要注意不同的数据库对于in语句的限制，比如mysql最多1000个，我们目前不会有这么多设备，但是还是需要注意可以改进。
            deviceMapper.deleteDevicesByIds(ids, userId);

            // 准备 MQTT 主题列表
            List<String> topics = ids.stream()
                    .map(id -> "home/device/" + id + "/data")
                    .toList();

            // 批量取消 MQTT 订阅
            mqttService.unsubscribe(topics);

            // 批量删除订阅记录
            mqttSubscriptionMapper.deleteSubscriptions(topics, userId);

            // 批量记录用户活动
            List<UserActivity> activities = ids.stream().map(id -> {
                UserActivity activity = new UserActivity();
                activity.setUserId(userId);
                activity.setUsername(username);
                activity.setDetails("Delete device by user '" + username +
                        "', device id is '" + id +
                        "', at time " + timestamp);
                return activity;
            }).collect(Collectors.toList());
            // 批量插入用户活动
            userActivityService.logUserActivities(activities);

            return ResponseEntity.ok(Result.success("Device deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting device", e);
            // TODO: 这里是不是不能吞掉异常，需要抛出异常，然后全局异常处理？不然会导致事物无法回滚？
            return ResponseEntity.badRequest().body(Result.error(405, "Error deleting device"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Result> controlDevice(Long id, String command) {
        command = command.toUpperCase(); // 转换为大写
        Long userId = userService.getCurrentUserId();
        Device device = deviceMapper.findDeviceById(id);
        if (device == null || !device.getUserId().equals(userId)) {
            logger.error("User does not have permission to operate this device");
            return ResponseEntity.badRequest().body(Result.error(405, "Device not found"));
        }

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
            logger.error("MQTT publish failed error controlling device", e);
            //TODO: 这里是不是不能吞掉异常，需要抛出异常，然后全局异常处理？不然会导致事物无法回滚？
            return ResponseEntity.badRequest().body(Result.error(405, "Error controlling device"));
        }

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

    @Transactional
    protected void initializeDeviceMqttSettings(Device device) throws MqttException {
        // 添加设备时发布设备初始状态，以保持一致性
        String statusTopic = "home/device/" + device.getId() + "/status";
        mqttService.publish(statusTopic, "OFF");

        // 订阅设备端的publish主题，可以接收到设备端传过来的数据
        mqttService.subscribe("home/device/" + device.getId() + "/data");

        // 添加订阅的主题到数据库
        MqttSubscription mqttSubscription = new MqttSubscription();
        mqttSubscription.setTopic("home/device/" + device.getId() + "/data");
        mqttSubscription.setUserId(device.getUserId());
        try {
            mqttSubscriptionMapper.insert(mqttSubscription);
        } catch (Exception e) {
            logger.error("Error adding mqtt subscription", e);
            throw new RuntimeException(e);
        }
    }

}
