package com.zy.zyxy.model.domain.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-23 0:42
 * 退出队伍请求
 */
@Data
public class TeamQuitRequest implements Serializable {
    private Long teamId;
    private static final long serialVersionUID = 1L;
}
