package com.zy.zyxy.model.vo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 标签视图
 * @TableName tag
 */
@Data
public class TagVO implements Serializable {


    /**
     * 标签名
     */
    private String tagName;



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