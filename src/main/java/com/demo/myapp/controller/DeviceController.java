package com.demo.myapp.controller;

import com.demo.myapp.controller.response.Result;
import com.demo.myapp.pojo.Device;
import com.demo.myapp.pojo.UserActivity;
import com.demo.myapp.service.DeviceService;
import com.demo.myapp.service.UserActivityService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
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
    private PagedResourcesAssembler<Device> pagedResourcesAssembler; // 分页资源装配器
    @Resource
    private UserActivityService userActivityService;

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
    public PagedModel<EntityModel<Device>> getDevicesByPage(@PathVariable int page, @PathVariable int size) {
        // Spring Data 的分页索引从 0 开始，而前端通常从 1 开始，所以这里需要处理一下，避免出现 page < 0 的情况
        if (page < 0) {
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Device> devicePage = deviceService.getDevicesByPage(pageable);
        return pagedResourcesAssembler.toModel(devicePage);
    }

    @GetMapping("/count")
    public long countDevices() {
        return deviceService.countDevices();
    }

    @GetMapping("/online")
    public long getOnlineDevices() {
        return deviceService.getOnlineDevices();
    }

    @GetMapping("/offline")
    public long getOfflineDevices() {
        return deviceService.getOfflineDevices();
    }

    @GetMapping("/recent-activities")
    public List<UserActivity> getUserActivities() {
        return userActivityService.getUserActivities();
    }
}

