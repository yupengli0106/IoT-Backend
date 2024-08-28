package com.demo.myapp.pojo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Yupeng Li
 * @Date: 28/8/2024 17:21
 * @Description:
 */

@Data
@AllArgsConstructor
@Entity
@NoArgsConstructor
public class MqttSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String topic;
    private long userId;
}
