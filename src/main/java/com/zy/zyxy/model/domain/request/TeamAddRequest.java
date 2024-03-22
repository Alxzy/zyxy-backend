package com.zy.zyxy.model.domain.request;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍添加请求
 */
@Data
public class TeamAddRequest implements Serializable {


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
     * 房间密码(私有)
     */
    private String userPassword;

    /**
     * 超时时间
     */
    private Date expireTime;



    private static final long serialVersionUID = 1L;


}