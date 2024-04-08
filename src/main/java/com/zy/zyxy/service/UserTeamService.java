package com.zy.zyxy.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zy.zyxy.model.dto.Team;
import com.zy.zyxy.model.dto.UserTeam;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Administrator
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service
* @createDate 2024-03-22 21:23:28
*/
public interface UserTeamService extends IService<UserTeam> {

    /**
     * 获取对应队伍已加入的用户数量
     * @param teamId
     * @return
     */
    Integer getTeamHasJoinUserNum(Long teamId);

    /**
     * 当前用户是否在队伍中
     * @param teamId
     * @param userId
     * @return
     */
    Boolean isUserInTeam(Long teamId,Long userId);
}
