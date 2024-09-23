package com.demo.myapp.controller;

import com.demo.myapp.dto.DeviceStatsDTO;
import com.demo.myapp.controller.response.Result;
import com.demo.myapp.pojo.Device;
import com.demo.myapp.pojo.Energy;
import com.demo.myapp.pojo.RestPage;
import com.demo.myapp.pojo.UserActivity;
import com.demo.myapp.service.DeviceService;
import com.demo.myapp.service.UserActivityService;
import com.demo.myapp.service.impl.UserService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * @Author: Yupeng Li
 * @Date: 11/7/2024 23:31
 * @Description:
 */
import java.util.List;

@RestController
@RequestMapping("/devices")
public class DeviceController {
    @Resource
    private DeviceService deviceService;
    @Resource
    private UserActivityService userActivityService;
    @Resource
    UserService userService;

    @GetMapping("/get_all")
    public List<Device> getAllDevices() {
        return deviceService.getAllDevices();
    }

    @GetMapping("/{id}")
    public Device getDeviceById(@PathVariable Long id) {
        return deviceService.getDeviceById(id);
    }

    @PostMapping("/add")
    public ResponseEntity<Result> addDevice(@RequestBody Device device) {
        return deviceService.addDevice(device);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result> updateDevice(@PathVariable Long id, @RequestBody Device device) {
        return deviceService.updateDevice(id, device);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Result> deleteDevice(@RequestBody List<Long> ids) {
        return deviceService.deleteDevice(ids);
    }

    @PostMapping("/{id}/control")
    public ResponseEntity<Result> controlDevice(@PathVariable Long id, @RequestParam String command) {
        return deviceService.controlDevice(id, command);
    }

    @GetMapping("/page/{page}/size/{size}")
    public RestPage<Device> getDevicesByPage(@PathVariable int page, @PathVariable int size) {
        long userId = userService.getCurrentUserId();
        // 使用 Math.max 来保证 page 至少为 0
        Pageable pageable = PageRequest.of(Math.max(page, 0), size);

        // 调用 service 层的方法获取分页数据
        Page<Device> devicePage = deviceService.getDevicesByPage(pageable, userId);

        // 将 Page 转换为 RestPage 为了反序列化
        return new RestPage<>(devicePage.getContent(), pageable.getPageNumber(), pageable.getPageSize(), devicePage.getTotalElements());
    }

    @GetMapping("/deviceStats")
    public ResponseEntity<DeviceStatsDTO> getDeviceStats() {
        long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(deviceService.getDeviceStats(userId));
    }

    @GetMapping("/recent-activities")
    public List<UserActivity> getUserActivities() {
        Long userId = userService.getCurrentUserId();
        return userActivityService.getUserActivities(userId);
    }

    @GetMapping("/energy")
    public List<Energy> getAllEnergy() {
        return deviceService.getAllEnergy();
    }

}

