package com.zy.zyxy.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍+用户视图
 * @TableName team
 */
@Data
public class TeamUserVO implements Serializable {


    /**
     * id
     */
    private Long id;

    /**
     * 创建者id
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
     *
     */
    private Date createTime;

    /**
     * 队长用户视图
     */
    private UserVO createUserVO;

    /**
     * 队伍加入用户人数
     */
    private Integer hasJoinNum;

    /**
     * 是否加入(用户未登录则全部未加入)
     */
    private Boolean hasJoin = false;

}