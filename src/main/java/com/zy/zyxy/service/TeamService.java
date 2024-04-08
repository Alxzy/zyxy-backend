package com.zy.zyxy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zy.zyxy.model.domain.request.TeamQueryRequest;
import com.zy.zyxy.model.dto.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.model.vo.TeamUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    /**
     * 根据查询请求查询队伍以及队长信息
     * @param teamQueryRequest
     * @return
     */
    List<TeamUserVO> listTeams(TeamQueryRequest teamQueryRequest,HttpServletRequest request,boolean isMe);

    /**
     * 添加用户到对应队伍
     * @param teamId
     * @param loginUser
     * @param password
     * @return
     */
    boolean joinTeam(Long teamId, User loginUser, String password);

    /**
     * 用户退出队伍
     * @param teamId
     * @param loginUser
     * @return
     */
    boolean quitTeam(Long teamId, User loginUser);

    /**
     * 队长解散队伍
     * @param teamId
     * @param loginUser
     * @return
     */
    boolean removeTeam(Long teamId, User loginUser);

    /**
     * 根据数据扩充 队伍-用户列表 计算并添加其他属性
     * @param resultList
     * @return
     */
    List<TeamUserVO> getResultTeamVOList(List<TeamUserVO> resultList,HttpServletRequest request);
}
