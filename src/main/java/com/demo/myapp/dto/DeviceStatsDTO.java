package com.demo.myapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Yupeng Li
 * @Date: 23/9/2024 15:56
 * @Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceStatsDTO {
    private long totalDevices;
    private long onlineDevices;
    private long offlineDevices;
}
