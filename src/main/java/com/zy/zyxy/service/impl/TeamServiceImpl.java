package com.zy.zyxy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zy.zyxy.model.dto.Team;
import com.zy.zyxy.mapper.TeamMapper;
import com.zy.zyxy.service.TeamService;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2024-03-22 21:20:39
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService {

}




