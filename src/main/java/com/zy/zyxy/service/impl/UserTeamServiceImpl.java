package com.zy.zyxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zy.zyxy.common.ErrorCode;
import com.zy.zyxy.exception.BusinessException;
import com.zy.zyxy.model.dto.UserTeam;
import com.zy.zyxy.service.UserTeamService;
import com.zy.zyxy.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2024-03-22 21:23:28
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

    @Override
    public Integer getTeamHasJoinUserNum(Long teamId) {
        // 此方法必须校验过队伍存在
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        int count = 0;
        try {
            count = (int)this.count(userTeamQueryWrapper);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        return count;
    }

    @Override
    public Boolean isUserInTeam(Long teamId, Long userId) {
        // 此方法必须 队伍和用户都存在
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId",userId);
        userTeamQueryWrapper.eq("teamId",teamId);
        int count = 0;
        try {
            count = (int)this.count(userTeamQueryWrapper);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return count > 0 ? Boolean.TRUE: Boolean.FALSE;
    }
}




