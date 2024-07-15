package com.demo.myapp.controller;

import com.demo.myapp.pojo.Device;
import com.demo.myapp.service.DeviceService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
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

    @GetMapping("/get_all")
    public List<Device> getAllDevices() {
        return deviceService.getAllDevices();
    }

    @GetMapping("/{id}")
    public Device getDeviceById(@PathVariable Long id) {
        return deviceService.getDeviceById(id);
    }

    @PostMapping("/add")
    public void addDevice(@RequestBody Device device) {
        deviceService.addDevice(device);
    }

    @PutMapping("/{id}")
    public void updateDevice(@PathVariable Long id, @RequestBody Device device) {
        deviceService.updateDevice(id, device);
    }

    @DeleteMapping("/{id}")
    public void deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
    }

    @PostMapping("/{id}/control")
    public void controlDevice(@PathVariable Long id, @RequestParam String command) {
        deviceService.controlDevice(id, command);
    }

    @GetMapping("/page/{page}/size/{size}")
    public PagedModel<EntityModel<Device>> getDevicesByPage(@PathVariable int page, @PathVariable int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Device> devicePage = deviceService.getDevicesByPage(pageable);
        return pagedResourcesAssembler.toModel(devicePage);
    }
}

