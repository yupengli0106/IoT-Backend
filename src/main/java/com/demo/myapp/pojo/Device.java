package com.demo.myapp.pojo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;


/**
 * @Author: Yupeng Li
 * @Date: 11/7/2024 23:02
 * @Description:
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String type;
    private String status;
    private Long userId;
    private Timestamp updateTime; //mysql会自动更新不需要设置，这里只是要返回给前端这个字段
}
