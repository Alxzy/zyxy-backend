package com.zy.zyxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zy.zyxy.common.ErrorCode;
import com.zy.zyxy.exception.BusinessException;
import com.zy.zyxy.model.dto.Team;
import com.zy.zyxy.mapper.TeamMapper;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.model.dto.UserTeam;
import com.zy.zyxy.model.enums.TeamStatusEnum;
import com.zy.zyxy.service.TeamService;
import com.zy.zyxy.service.UserService;
import com.zy.zyxy.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.Optional;

import static com.zy.zyxy.contant.TeamConstant.DEFAULT_MAX_TEAM_NUM;

/**
* @author Administrator
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2024-03-22 21:20:39
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService {

    @Resource
    UserService userService;

    @Resource
    UserTeamService userTeamService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createTeam(Team team, HttpServletRequest request) {
        // 1.非参数校验
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.权限校验
        // 2.1 必须登录
        User loginUser = userService.getLoginUser(request);
        team.setCreateId(loginUser.getId());
        // 2.2 当前用户创建的队伍不能超过5个
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("createId",loginUser.getId());
        int alreadyCreateTeamCount = (int)this.count(teamQueryWrapper);
        if(alreadyCreateTeamCount >= DEFAULT_MAX_TEAM_NUM){
            throw new BusinessException(ErrorCode.NO_AUTH,"用户创建的队伍个数已达到上限");
        }
        // 3.参数校验
        // 3.1 队伍人数 > 1 且 <= 20
        Integer peopleNum = Optional.ofNullable(team.getPeopleNum()).orElse(0);
        if(peopleNum <= 1 || peopleNum > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍人数不符合规则");
        }
        // 3.2 队伍状态 必需有 或者 为 null 默认为 0(公开)
        Integer status = Optional.ofNullable(team.getStatus()).orElse(0);
        if(TeamStatusEnum.getEnumByValue(status) == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍状态错误");
        }
        // 3.3 队伍名称不为空 <= 20
        String name = team.getName();
        if(StringUtils.isBlank(name) || name.length() > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍名称不满足要求");
        }
        // 3.4 队伍状态如果为私密 密码不能为空，密码长度  >= 4 且<= 20
        String userPassword = team.getUserPassword();
        if(TeamStatusEnum.getEnumByValue(status) == TeamStatusEnum.SECRET){
            if(StringUtils.isBlank(userPassword) || userPassword.length() < 4 || userPassword.length() > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码格式有误");
            }
        }
        // 3.5 描述 <= 512
        String description = team.getDescription();
        if(StringUtils.isBlank(description) || description.length() > 512){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍描述不满足要求");
        }
        // 3.6 过期时间不为空且大于当前时间
        Date expireTime = team.getExpireTime();
        if(expireTime == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"过期时间不能为空");
        }
        if(new Date().after(expireTime)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"过期时间过短");
        }
        // 4.插入队伍信息到队伍表
        team.setId(null);
        boolean result = this.save(team);
        if(!result || team.getId() == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"创建队伍失败");
        }
        // 5.插入用户-队伍信息到用户-队伍表
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(team.getId());
        userTeam.setUserId(team.getCreateId());
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"创建队伍失败");
        }
        return team.getId();
    }
}




