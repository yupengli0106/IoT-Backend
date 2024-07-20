package com.demo.myapp.service;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.pojo.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * @Author: Yupeng Li
 * @Date: 11/7/2024 23:19
 * @Description:
 */
public interface DeviceService {
    List<Device> getAllDevices();
    Device getDeviceById(Long id);
    ResponseEntity<Result> addDevice(Device device);
    ResponseEntity<Result> updateDevice(Long id, Device device);
    void deleteDevice(Long id);
    void controlDevice(Long id, String command);
    Page<Device> getDevicesByPage(Pageable pageable);
}
