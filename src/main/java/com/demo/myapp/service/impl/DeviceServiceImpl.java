package com.demo.myapp.service.impl;

import com.demo.myapp.dto.DeviceStatsDTO;
import com.demo.myapp.controller.response.Result;
import com.demo.myapp.exception.DeviceOperationException;
import com.demo.myapp.mapper.DeviceMapper;
import com.demo.myapp.mapper.EnergyMapper;
import com.demo.myapp.mapper.MqttSubscriptionMapper;
import com.demo.myapp.pojo.*;
import com.demo.myapp.service.DeviceService;
import com.demo.myapp.service.UserActivityService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @Resource
    private CachedDeviceService cachedDeviceService;

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
    public ResponseEntity<Result> addDevice(@Valid Device device) {
        // 获取当前登录用户的ID
        Long userId = userService.getCurrentUserId();
        String username = userService.getCurrentUsername(); // Retrieve once

        cachedDeviceService.clearDeviceStatsCache(userId); // !清除设备状态统计缓存
        cachedDeviceService.clearDevicesByPageCache(userId); // !清除设备分页缓存
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
                    username,
                    device.getName(),
                    "Add device by user '" +
                            username +
                            "', default status is 'OFF', at time " +
                            LocalDateTime.now().format(DATE_TIME_FORMATTER)
            );

            //TODO: 这里需要配合一个写死的模拟器来使用，后续可以改进
            initializeDeviceMqttSettings(device);

            return ResponseEntity.ok(Result.success("Device added successfully"));
        } catch (DuplicateKeyException e) {
            logger.error("Device name already exists in the database", e);
            throw new DeviceOperationException("Device name already exists", e);
        } catch (DataAccessException e) {
            logger.error("Database error while adding device", e);
            throw new DeviceOperationException("Database error while adding device", e);
        } catch (MqttException e) {
            logger.error("MQTT error while adding device", e);
            throw new DeviceOperationException("Failed to initialize MQTT settings for the device", e);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Result> updateDevice(Long id, Device device) {
        Long userId = userService.getCurrentUserId();
        String username = userService.getCurrentUsername();
        cachedDeviceService.clearDevicesByPageCache(userId); // !清除设备分页缓存

        Device existingDevice = deviceMapper.findDeviceByName(device.getName(), userId);
        // 检查设备名称是否重复(排除当前设备)
        if (existingDevice != null && !existingDevice.getId().equals(id)) {
            return ResponseEntity.badRequest().body(Result.error(409, "Device name already exists"));
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
                    username,
                    device.getName(),
                    "Update device by user '" +
                            username
                            + "', device name is '" + device.getName()
                            + "', device type is '" + device.getType()
                            + "' at time " + LocalDateTime.now().format(DATE_TIME_FORMATTER)
            );

            return ResponseEntity.ok(Result.success("Device updated successfully"));
        } catch (DuplicateKeyException e) {
            logger.error("Device name already exists in the database", e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Result.error(409, "Device name already exists"));
        } catch (DataAccessException e) {
            logger.error("Database error while updating device", e);
            throw new DeviceOperationException("Database error while updating device", e);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Result> deleteDevice(List<Long> ids) {
        Long userId = userService.getCurrentUserId();
        String username = userService.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();
        String timestamp = DATE_TIME_FORMATTER.format(now);

        cachedDeviceService.clearDeviceStatsCache(userId); // !清除设备状态统计缓存
        cachedDeviceService.clearDevicesByPageCache(userId); // !清除设备分页缓存

        try {
            // 批量删除设备(每次最多删除900个设备，防止数据库限制报错)
            int batchSize = 900; // Safe batch size
            for (int i = 0; i < ids.size(); i += batchSize) {
                List<Long> batchIds = ids.subList(i, Math.min(i + batchSize, ids.size()));
                deviceMapper.deleteDevicesByIds(batchIds, userId);
            }

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
        } catch (DataAccessException e) {
            logger.error("Database error while deleting device", e);
            throw new DeviceOperationException("Database error while deleting device", e);
        } catch (MqttException e) {
            logger.error("MQTT error while deleting device", e);
            throw new DeviceOperationException("Failed to unsubscribe MQTT topics", e);
        } catch (Exception e) {
            logger.error("Error deleting device", e);
            throw new DeviceOperationException("Error deleting device", e);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Result> controlDevice(Long id, String command) {
        command = command.toUpperCase(); // 转换为大写
        Long userId = userService.getCurrentUserId();
        String username = userService.getCurrentUsername();
        cachedDeviceService.clearDeviceStatsCache(userId); // !清除设备状态统计缓存
        cachedDeviceService.clearDevicesByPageCache(userId); // !清除设备分页缓存
        Device device = deviceMapper.findDeviceById(id);

        if (device == null) {// 设备不存在
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.error(404, "Device not found"));
        }
        if (!device.getUserId().equals(userId)) {// 设备不属于当前用户
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(403, "Access denied"));
        }

        try {
            device.setUserId(userId);
            device.setStatus(command);
            // 使用MQTT发送控制命令
            String topic = "home/device/" + device.getId() + "/status";
            mqttService.publish(topic, command);// 发布消息
            deviceMapper.updateDeviceStatus(device); // 更新设备状态在数据库
            //记录用户操作
            userActivityService.logUserActivity(
                    userId,
                    username,
                    device.getName(),
                    "Controlled device by user '" +
                            username +
                            "', device name is '" + device.getName() +
                            "', command is '" + command +
                            "', at time " + LocalDateTime.now().format(DATE_TIME_FORMATTER)
            );
        } catch (DataAccessException e) {
            logger.error("Database error while controlling device", e);
            throw new DeviceOperationException("Database error while controlling device", e);
        } catch (Exception e) {
            logger.error("Error controlling device", e);
            throw new DeviceOperationException("Error controlling device", e);
        }

        return ResponseEntity.ok(Result.success("Device controlled successfully"));

    }

    @Override
    @Cacheable(value = "devicesByPage",
            key = "'devicesByPage_' + #currentUserId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize",
            condition = "#pageable.pageNumber <= 3")
    public RestPage<Device> getDevicesByPage(Pageable pageable, long currentUserId) { // !仅缓存前三页热点数据
        // 分页参数
        int pageSize = pageable.getPageSize();
        int offset = pageable.getPageNumber() * pageSize;

        // 查询设备总数
        long totalDevices = deviceMapper.countDevices(currentUserId);
        if (offset >= totalDevices) { // 如果偏移量大于等于设备总数，返回空列表
            return new RestPage<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0);
        }

        // 查询分页设备列表
        List<Device> devices = deviceMapper.findDevicesByPage(currentUserId,pageSize, offset);

        // ！使用RestPage封装分页数据,继承了PageImpl.这样做是为了在cache的时候可以正确的被序列化
        return new RestPage<>(devices, pageable.getPageNumber(), pageable.getPageSize(), totalDevices);
    }


    @Override
    public List<Energy> getAllEnergy() {// TODO: can be cached?
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
        } catch (DataAccessException e) {
            logger.error("Error inserting MQTT subscription", e);
            throw new DeviceOperationException("Error inserting MQTT subscription", e);
        } catch (Exception e) {
            logger.error("Error initializing MQTT settings", e);
            throw new DeviceOperationException("Error initializing MQTT settings", e);
        }
    }

    /**
     * 获取设备状态统计：总设备数、在线设备数、离线设备数
     * @param userId 用户ID
     * @return 设备状态统计
     */
    @Override
    @Cacheable(value = "deviceStats", key = "'deviceStats_' + #userId")
    public DeviceStatsDTO getDeviceStats(long userId) {
        return deviceMapper.getDeviceStats(userId);
    }

}
