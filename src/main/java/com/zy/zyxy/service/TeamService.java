package com.zy.zyxy.service;

import com.zy.zyxy.model.dto.Team;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;

/**
* @author Administrator
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-03-22 21:20:39
*/
public interface TeamService extends IService<Team> {

    /**
     * 根据队伍信息和当前登录用户创建队伍
     * @param team
     * @param request
     */
    long createTeam(Team team, HttpServletRequest request);
}
