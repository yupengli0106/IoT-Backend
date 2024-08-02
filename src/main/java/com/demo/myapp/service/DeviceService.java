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
public interface  DeviceService {
    List<Device> getAllDevices();
    Device getDeviceById(Long id);
    ResponseEntity<Result> addDevice(Device device);
    ResponseEntity<Result> updateDevice(Long id, Device device);
    ResponseEntity<Result> deleteDevice(List<Long> id);
    ResponseEntity<Result> controlDevice(Long id, String command);
    Page<Device> getDevicesByPage(Pageable pageable);

    long countDevices();

    long getOnlineDevices();

    long getOfflineDevices();
}
