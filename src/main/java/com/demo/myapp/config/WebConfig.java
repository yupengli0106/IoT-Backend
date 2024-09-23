package com.demo.myapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * @Author: Yupeng Li
 * @Date: 23/9/2024 18:20
 * @Description:
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
//开启Spring Data Web支持 通过DTO序列化 分页 和 排序 信息 默认是PageSerializationMode.VIA_WRAPPER
public class WebConfig {
}
