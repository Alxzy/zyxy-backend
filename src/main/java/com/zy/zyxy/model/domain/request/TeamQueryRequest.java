package com.zy.zyxy.model.domain.request;

import com.baomidou.mybatisplus.annotation.*;
import com.zy.zyxy.common.PageRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 队伍
 * @TableName team
 */
@TableName(value ="team")
@Data
public class TeamQueryRequest extends PageRequest implements Serializable {

    /**
     * id列表
     */
    private List<Long> idList;

    /**
     * 队长id
     */
    private Long createId;

    /**
     * 队伍最大人数
     */
    private Integer peopleNum;

    /**
     * 是否公开(0- 公开 1 - 私有 2 -加密)
     */
    private Integer status;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 超时时间
     */
    private Date expireTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 搜索关键词
     */
    private String searchText;


    private static final long serialVersionUID = 1L;



}