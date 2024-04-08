package com.zy.zyxy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zy.zyxy.common.ErrorCode;
import com.zy.zyxy.exception.BusinessException;
import com.zy.zyxy.model.domain.request.InviteRequest;
import com.zy.zyxy.model.dto.Invitation;
import com.zy.zyxy.model.dto.Team;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.model.dto.UserTeam;
import com.zy.zyxy.model.vo.TeamUserVO;
import com.zy.zyxy.model.vo.UserVO;
import com.zy.zyxy.service.InvitationService;
import com.zy.zyxy.mapper.InvitationMapper;
import com.zy.zyxy.service.TeamService;
import com.zy.zyxy.service.UserService;
import com.zy.zyxy.service.UserTeamService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zy.zyxy.contant.TeamConstant.DEFAULT_MAX_TEAM_NUM;
import static com.zy.zyxy.contant.UserConstant.USER_JOIN_TEAM_USER_KEY;

/**
* @author Administrator
* @description 针对表【invitation(邀请关系)】的数据库操作Service实现
* @createDate 2024-04-07 21:16:42
*/
@Service
public class InvitationServiceImpl extends ServiceImpl<InvitationMapper, Invitation>
    implements InvitationService{

    @Resource
    private TeamService teamService;

    @Resource
    private UserService userService;

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public List<TeamUserVO> getMyInvitation(HttpServletRequest request) {
        // 1.获取登录用户
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        // 2.获取邀请数据
        QueryWrapper<Invitation> invitationQueryWrapper = new QueryWrapper<>();
        invitationQueryWrapper.eq("userId", userId);
        List<Invitation> invitationList = this.list(invitationQueryWrapper);
        // 3.关联获取队伍数据
        List<Long> teamIdList = invitationList.stream().map(Invitation::getTeamId).collect(Collectors.toList());
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        if(!CollectionUtils.isNotEmpty(teamIdList)){
            return new ArrayList<>();
        }
        teamQueryWrapper.in("id",teamIdList);
        // 3.1 不展示过期队伍
        teamQueryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList = teamService.list(teamQueryWrapper);
        // 3.2 转成TeamUserVO 列表
        List<TeamUserVO> teamUserVOList = new ArrayList();
        for (Team team1 : teamList) {
            userId = team1.getCreateId();
            if (userId == null) {
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team1, teamUserVO);
            // 脱敏用户信息,并添加到队伍视图中
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUserVO(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        List<TeamUserVO> resultList = teamService.getResultTeamVOList(teamUserVOList, request);
        return resultList;
    }

    @Override
    public Boolean inviteRequest(InviteRequest inviteRequest, User loginUser) {
        // 1.用户校验
        // 1.1必须登录,获取登录用户id
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 1.2 邀请用户必须存在
        // 非空校验
        Long invitedUserId = inviteRequest.getUserId();
        Long inviteTeamId = inviteRequest.getTeamId();
        if(inviteTeamId == null || invitedUserId == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User invitedUser = userService.getById(invitedUserId);
        if(invitedUser == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"被邀请用户不存在");
        }
        // 2.队伍校验
        // 2.1 队伍必须存在且未过期
        Team team = teamService.getById(inviteTeamId);
        if(team == null || team.getExpireTime().before(new Date())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍不存在或者已过期");
        }
        // 2.2 队伍人数未满
        Integer joinUserNum = userTeamService.getTeamHasJoinUserNum(team.getId());
        Integer peopleNum = team.getPeopleNum();
        if(joinUserNum >= peopleNum){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍人数已满");
        }

        // 3.其他校验
        // 3.1 登录用户是当前队伍房主
        Long loginUserId = loginUser.getId();
        // Long 类型比较相等必须使用 equals
        if(!loginUserId.equals(team.getCreateId())){
            throw new BusinessException(ErrorCode.NO_AUTH,"只有房主可以邀请其他用户");
        }
        // 3.2 被邀请用户必须不在队伍中
        Boolean userInTeam = userTeamService.isUserInTeam(inviteTeamId, invitedUserId);
        if(userInTeam){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"邀请用户已经在队伍中");
        }
        // 3.3 已经邀请过不再可以邀请
        // todo 线程安全问题(单用户多次邀请) 用户锁
        Invitation invitation = new Invitation();
        BeanUtils.copyProperties(inviteRequest,invitation);
        long count = this.count(new QueryWrapper<Invitation>(invitation));
        if(count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"已邀请,请勿重复邀请");
        }
        // 4.插入一条邀请

        boolean result = this.save(invitation);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean agree(Long loginUserId, Long teamId) {
        // todo 可以抽象一个方法出来 重复代码有点多了
        // 1.校验参数
        if(loginUserId <= 0 || teamId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.业务校验
        // 2.1 队伍必须存在且未过期
        Team team = teamService.getById(teamId);
        if(team == null || team.getExpireTime().before(new Date())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍不存在或者已过期");
        }
        // 2.2 队伍人数未满
        // todo 队伍人数超额 线程安全问题
        Integer joinUserNum = userTeamService.getTeamHasJoinUserNum(team.getId());
        Integer peopleNum = team.getPeopleNum();
        if(joinUserNum >= peopleNum){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍人数已满");
        }
        // 2.3 被邀请用户必须不在队伍中
        Boolean userInTeam = userTeamService.isUserInTeam(teamId, loginUserId);
        if(userInTeam){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"您已经在队伍中");
        }
        // 2.4 邀请必须存在
        QueryWrapper<Invitation> invitationQueryWrapper = new QueryWrapper<>();
        invitationQueryWrapper.eq("userId",loginUserId);
        invitationQueryWrapper.eq("teamId",teamId);
        Invitation invitation = this.getOne(invitationQueryWrapper);
        if(invitation == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"邀请不存在");
        }
        String userKey = String.format(USER_JOIN_TEAM_USER_KEY, loginUserId);
        RLock userLock = redissonClient.getLock(userKey);
        // 抢分布式锁(用户锁)
        try {
            int getCount = 0;
            // 忙等待锁
            while (true){
                getCount ++;
                if(userLock.tryLock(0,-1, TimeUnit.MILLISECONDS)){
                    System.out.println("getLock: " + Thread.currentThread().getId());
                    // 2.4 用户加入的队伍不能超过上限
                    // 用户最多加入5个队伍
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", loginUserId);
                    long count = userTeamService.count(userTeamQueryWrapper);
                    if (count >= DEFAULT_MAX_TEAM_NUM) {
                        throw new BusinessException(ErrorCode.NO_AUTH, "用户已加入(创建)队伍到达上限");
                    }
                    // 3. 执行操作
                    // 3.1 添加用户队伍关系
                    // 添加记录到用户-队伍表
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(loginUserId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    boolean save = userTeamService.save(userTeam);
                    if(!save){
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR,"添加队伍失败");
                    }
                    // 3.2 删除邀请
                    boolean remove = this.remove(invitationQueryWrapper);
                    if(!remove){
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除邀请失败");
                    }
                    return remove && save;

                }
                // 不能无限循环
                if(getCount > 5){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
            }

        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } finally {
            // 只能释放自己的锁
            if (userLock.isHeldByCurrentThread()) {
                System.out.println("unLock: userLock:  " + Thread.currentThread().getId());
                userLock.unlock();
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR);
    }
}




