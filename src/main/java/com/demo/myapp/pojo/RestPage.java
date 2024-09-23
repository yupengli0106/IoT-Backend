package com.demo.myapp.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * 用来处理分页数据的类，继承自PageImpl。
 * 由于PageImpl不能直接反序列化，所以需要自定义反序列化方法。
 * 通过@JsonCreator注解，指定反序列化方法，将分页数据反序列化为RestPage对象。
 * 通过@JsonProperty注解，指定反序列化时的属性名。
 * 通过@JsonIgnoreProperties注解，忽略分页对象中的复杂属性。
 * @param <T> 分页数据的类型
 * Description: 用来处理分页数据的类，继承自PageImpl。
 */
@JsonIgnoreProperties(ignoreUnknown = true, value = {"pageable"}) // 忽略分页对象中的复杂属性
public class RestPage<T> extends PageImpl<T> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RestPage(@JsonProperty("content") List<T> content,
                    @JsonProperty("number") int page,
                    @JsonProperty("size") int size,
                    @JsonProperty("totalElements") long total) {
        super(content, PageRequest.of(page, size), total);
    }

    public RestPage(PageImpl<T> page) {
        super(page.getContent(), page.getPageable(), page.getTotalElements());
    }
}
