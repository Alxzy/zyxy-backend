package com.zy.zyxy.model.domain.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 标签查询请求
 * @TableName tag
 */
@Data
public class TagQueryRequest implements Serializable {


    /**
     * id列表
     */
    private List<Long> idList;

    /**
     * 标签名
     */
    private String tagName;

    /**
     * 上传标签的用户
     */
    private Long userId;

    /**
     * 父标签id
     */
    private Integer parentId;

    /**
     * 是否为父标签 0-不是父标签 1-父标签
     */
    private Integer isParent;

    /**
     * 分类
     */
    private String category;

    /**
     * 创建时间
     */
    private Date createTime;





}