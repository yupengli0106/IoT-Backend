package com.demo.myapp.service.impl;

import com.demo.myapp.mapper.DeviceMapper;
import com.demo.myapp.pojo.Device;
import com.demo.myapp.service.DeviceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

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
    private RabbitMQProducerService rabbitMQProducerService;

    @Override
    public List<Device> getAllDevices() {
        return deviceMapper.findAllDevices();
    }

    @Override
    public Device getDeviceById(Long id) {
        return deviceMapper.findDeviceById(id);
    }

    @Override
    public void addDevice(Device device) {
        deviceMapper.insertDevice(device);
    }

    @Override
    public void updateDevice(Long id, Device device) {
        device.setId(id);
        deviceMapper.updateDevice(device);
    }

    @Override
    public void deleteDevice(Long id) {
        deviceMapper.deleteDeviceById(id);
    }

    @Override
    public void controlDevice(Long id, String command) {
        Device device = deviceMapper.findDeviceById(id);
        if (device != null) {
            device.setStatus(command);
            deviceMapper.updateDevice(device);
            // 使用RabbitMQ发送控制命令
            rabbitMQProducerService.sendMessage("Device ID: " + id + " Command: " + command);
        }
    }
}
