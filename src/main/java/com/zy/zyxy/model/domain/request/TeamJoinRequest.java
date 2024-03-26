package com.zy.zyxy.model.domain.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-26 20:14
 */
@Data
public class TeamJoinRequest implements Serializable{

    /**
     * 队伍id
     */
    private Long teamId;

    /**
     * 密码
     */
    private String password;


}
