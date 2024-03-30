package com.zy.zyxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zy.zyxy.common.ErrorCode;
import com.zy.zyxy.exception.BusinessException;
import com.zy.zyxy.model.domain.request.TeamQueryRequest;
import com.zy.zyxy.model.dto.Team;
import com.zy.zyxy.mapper.TeamMapper;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.model.dto.UserTeam;
import com.zy.zyxy.model.enums.TeamStatusEnum;
import com.zy.zyxy.model.vo.TeamUserVO;
import com.zy.zyxy.model.vo.UserVO;
import com.zy.zyxy.service.TeamService;
import com.zy.zyxy.service.UserService;
import com.zy.zyxy.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.*;

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
        // 3.1 队伍人数 >= 1 且 <= 20
        Integer peopleNum = Optional.ofNullable(team.getPeopleNum()).orElse(0);
        if(peopleNum < 1 || peopleNum > 20){
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

    @Override
    public List<TeamUserVO> listTeams(TeamQueryRequest teamQueryRequest,HttpServletRequest request,boolean isMe) {
        Team team = new Team();
        BeanUtils.copyProperties(teamQueryRequest, team);
        // 1.属性复制
        // 描述和队伍名称要进行模糊查询
        team.setName(null);
        team.setDescription(null);
        // 2.权限校验：只有管理员可以查询 非公开和加密房间
        if(!userService.isAdmin(request) && !isMe){
            if(TeamStatusEnum.PRIVATE.equals(TeamStatusEnum.getEnumByValue(team.getStatus()))){
                throw new BusinessException(ErrorCode.NO_AUTH,"普通用户不可访问私密房间");
            }
            // 加密也可以查好了
//            team.setStatus(TeamStatusEnum.PUBLIC.getValue());
        }
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        List<Long> idList = teamQueryRequest.getIdList();
        if (isMe && CollectionUtils.isNotEmpty(idList)) {
            queryWrapper.in("id", idList);
        }
        // 3.参数校验 不展示过期队伍
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        // 4.拼接 OR 子句 关键字 模糊查询
        String searchText = Optional.ofNullable(teamQueryRequest.getSearchText()).orElse("");
        queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
        // 5.查询
        List<Team> teamList = this.list(queryWrapper);
        // 6.关联查询
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        List<TeamUserVO> teamUserVOList = new ArrayList();
        for (Team team1 : teamList) {
            Long userId = team1.getCreateId();
            if(userId == null){
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team1,teamUserVO);
            // 脱敏用户信息,并添加到队伍视图中
            if(user != null){
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user,userVO);
                teamUserVO.setCreateUserVO(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }

        return teamUserVOList;
    }

    @Override
    public boolean joinTeam(Long teamId, User loginUser, String password) {
        // todo 还有一个逆天的问题,就是校验完后准备添加前队伍被删了,小概率事件
        // 参数校验
        Team team = isExistTeam(teamId);
        Integer maxPeopleNum = team.getPeopleNum();
        // 2.队伍不为私密状态
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(team.getStatus());
        if(TeamStatusEnum.PRIVATE.equals(statusEnum)){
            throw new BusinessException(ErrorCode.NO_AUTH,"无法加入该队伍");
        }
        // 3.队伍未过期
        if(isTeamExpire(teamId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已过期");
        }
        // 4.队伍加密必须密码匹配
        if(TeamStatusEnum.SECRET.equals(statusEnum)){
            String userPassword = team.getUserPassword();
            if(!userPassword.equals(password)){
                throw new BusinessException(ErrorCode.NO_AUTH,"密码错误");
            }
        }


        Long userId = loginUser.getId();

        // team-user相关校验
        // todo 并发请求时可能出现问题 线程安全 使用分布式锁
        // 1.用户最多加入5个队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        long count = userTeamService.count(userTeamQueryWrapper);
        if(count >= DEFAULT_MAX_TEAM_NUM){
            throw new BusinessException(ErrorCode.NO_AUTH,"用户已加入(创建)队伍到达上限");
        }
        // 2.用户不能重复加入队伍
        userTeamQueryWrapper.eq("teamId",teamId);
        count = userTeamService.count(userTeamQueryWrapper);
        if(count >= 1){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不能重复添加队伍");
        }
        // 3.队伍用户个数已满不能加入
        userTeamQueryWrapper = new QueryWrapper<UserTeam>();
        userTeamQueryWrapper.eq("teamId",teamId);
        count = userTeamService.count(userTeamQueryWrapper);
        if(count >= maxPeopleNum){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍人数已满");
        }

        // 添加记录到用户-队伍表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        return userTeamService.save(userTeam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(Long teamId, User loginUser) {
        // 参数校验
        // 1.队伍是否存在
        isExistTeam(teamId);
        // 2.是否加入队伍
        Long userId = loginUser.getId();
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId",teamId);
        // 3.队伍未过期
        if(isTeamExpire(teamId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已过期");
        }
        //获取队伍当前人数
        long currentPeople = userTeamService.count(userTeamQueryWrapper);
        userTeamQueryWrapper.eq("userId",userId);
        long count = userTeamService.count(userTeamQueryWrapper);
        if(count < 1){
            throw new BusinessException(ErrorCode.NO_AUTH,"用户不能退出未加入的队伍");
        }
        // 4.获取队伍当前人数
        if(currentPeople <= 1){
            // 删除队伍
            this.removeById(teamId);
        }else{
            // 还有其他人
            // 4.1 如果队长退出 转移给第二早加入的队伍
            if(isCreateUser(teamId,userId)){
                QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<UserTeam>();
                queryWrapper.eq("teamId",teamId);
                queryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
                UserTeam userTeam = userTeamList.get(1);
                Long newCreateUserId = userTeam.getUserId();
                // 更新当前队伍队长
                Team team = new Team();
                team.setCreateId(newCreateUserId);
                team.setId(teamId);
                boolean result = this.updateById(team);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
            // 4.2 非队长 退出即可
        }
        return userTeamService.remove(userTeamQueryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTeam(Long teamId, User loginUser) {
        // todo 解散队伍和加入队伍 并发问题
        // 参数校验
        // 1.队伍是否存在 后面方法里已经有了
//        isExistTeam(teamId);
        // 2.检查是否为队长
        Long userId = loginUser.getId();
        if(!isCreateUser(teamId,userId)){
            throw new BusinessException(ErrorCode.NO_AUTH,"只有队长用户可以解散队伍");
        }
        // 3.队伍未过期
        if(isTeamExpire(teamId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已过期");
        }
        // 4. 移除关联信息,再删除队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        // 删除队伍
        return this.removeById(teamId);
    }

    private Team isExistTeam(Long teamId) {
        if(teamId == null || teamId < 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍错误");
        }
        Team team = this.getById(teamId);
        if(team == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍不存在");
        }

        return team;
    }


    private Boolean isTeamExpire(Long teamId){
        Team team = this.isExistTeam(teamId);
        // 3.队伍未过期
        if(team.getExpireTime().before(new Date())){
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean isCreateUser(Long teamId,Long userId){
        Team team = this.isExistTeam(teamId);
        Long createId = team.getCreateId();
        return createId.equals(userId);
    }


}




