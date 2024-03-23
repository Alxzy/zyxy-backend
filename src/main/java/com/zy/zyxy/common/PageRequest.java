package com.zy.zyxy.common;

import lombok.Data;

import static com.zy.zyxy.contant.CommonConstant.SORT_ORDER_ASC;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-23 0:50
 * 统一分页请求 父类
 */
@Data
public class PageRequest {
    /**
     * 当前页号
     */
    private long current = 1;

    /**
     * 页面大小
     */
    private long pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序(默认升序)
     */
    private String sortOrder = SORT_ORDER_ASC;
}
