package com.zy.zyxy.model.domain.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邀请请求
 */
@Data
public class InviteRequest implements Serializable {


    /**
     * 被邀请用户id
     */
    private Long userId;

    /**
     * 加入队伍id
     */
    private Long teamId;



    private static final long serialVersionUID = 1L;


}