package com.zy.zyxy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zy.zyxy.annotation.AuthCheck;
import com.zy.zyxy.common.BaseResponse;
import com.zy.zyxy.common.DeleteRequest;
import com.zy.zyxy.common.ErrorCode;
import com.zy.zyxy.common.ResultUtils;
import com.zy.zyxy.exception.BusinessException;
import com.zy.zyxy.model.domain.request.*;
import com.zy.zyxy.model.dto.Team;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.model.dto.UserTeam;
import com.zy.zyxy.model.enums.TeamStatusEnum;
import com.zy.zyxy.model.vo.TeamUserVO;
import com.zy.zyxy.service.TeamService;
import com.zy.zyxy.service.UserService;
import com.zy.zyxy.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 队伍接口
 *
 * @author zy
 */
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://localhost:5173/"}, allowCredentials = "true")
@Slf4j
public class TeamController {

    @Resource
    private TeamService teamService;

    @Resource
    private UserService userService;

    @Resource
    private UserTeamService userTeamService;

    @PostMapping("/add")
    @AuthCheck(anyRole = {"admin","user","vip"})
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest,team);
        long teamId = teamService.createTeam(team, request);
        return ResultUtils.success(teamId);
    }



    @PutMapping("/update")
    @AuthCheck(anyRole = {"admin","user","vip"})
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamUpdateRequest.getId();
        if(teamId == null || teamId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.校验队伍是否存在
        long count = teamService.count(new QueryWrapper<Team>().eq("id", teamId));
        if(count < 1){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍不存在");
        }
        // 3.只有创建者和管理员可以修改
        Team oldTeam = teamService.getById(teamId);
        User loginUser = userService.getLoginUser(request);
        if(!userService.isAdmin(request) && !loginUser.getId().equals(oldTeam.getCreateId())){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        // 4.加密房间必须设置密码
        if(TeamStatusEnum.SECRET.equals(TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus()))){
            String userPassword = teamUpdateRequest.getUserPassword();
            if(StringUtils.isBlank(userPassword)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"加密房间必须设置密码");
            }
            if(StringUtils.isBlank(userPassword) || userPassword.length() < 4 || userPassword.length() > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码格式有误");
            }
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamUpdateRequest,team);
        boolean result = teamService.updateById(team);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(result);
    }

    @GetMapping("/get")
    @AuthCheck(anyRole = {"admin","user","vip"})
    public BaseResponse<Team> getTeamById(long id) {
        // todo 权限校验
        if (id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    @GetMapping("/list")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<List<Team>> listTeams(TeamQueryRequest teamQueryRequest) {
        if (teamQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQueryRequest,team);

        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>(team);
        List<Team> teamList = teamService.list(teamQueryWrapper);

        return ResultUtils.success(teamList);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<TeamUserVO>> listPageTeams(TeamQueryRequest teamQueryRequest,HttpServletRequest request) {
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<TeamUserVO> resultList = teamService.listTeams(teamQueryRequest,request,false);
        Page<TeamUserVO> resultPage = new Page<>(teamQueryRequest.getCurrent(),teamQueryRequest.getPageSize());
        resultPage.setRecords(resultList);
        return ResultUtils.success(resultPage);
    }

    @PostMapping("/join")
    @AuthCheck(anyRole = {"admin","user","vip"})
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest,HttpServletRequest request){
        if(teamJoinRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        String password = teamJoinRequest.getPassword();
        Long teamId = teamJoinRequest.getTeamId();
        boolean result = teamService.joinTeam(teamId,loginUser,password);
        return ResultUtils.success(result);
    }

    @PostMapping("/quit")
    @AuthCheck(anyRole = {"admin","user","vip"})
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request){
        if(teamQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long teamId = teamQuitRequest.getTeamId();
        boolean result = teamService.quitTeam(teamId,loginUser);
        return ResultUtils.success(result);
    }

    @DeleteMapping("/delete")
    @AuthCheck(anyRole = {"admin","user","vip"})
    public BaseResponse<Boolean> removeMyTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request){
        if(teamQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long teamId = teamQuitRequest.getTeamId();
        boolean result = teamService.removeTeam(teamId,loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 获取已加入的队伍
     * @param teamQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/my/join")
    @AuthCheck(anyRole = {"admin","user","vip"})
    public BaseResponse<Page<TeamUserVO>> listJoinTeams(TeamQueryRequest teamQueryRequest,HttpServletRequest request) {
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 关联查询所有队伍得到已加入队伍id
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        Long userId = userService.getLoginUser(request).getId();
        userTeamQueryWrapper.eq("userId", userId);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        Map<Long, List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        ArrayList<Long> idList = new ArrayList<>(listMap.keySet());
        // 按照 id列表查询
        teamQueryRequest.setIdList(idList);
        List<TeamUserVO> resultList = teamService.listTeams(teamQueryRequest,request,true);
        Page<TeamUserVO> resultPage = new Page<>(teamQueryRequest.getCurrent(),teamQueryRequest.getPageSize());
        resultPage.setRecords(resultList);
        return ResultUtils.success(resultPage);
    }

    /**
     * 获取当前是队长的队伍
     * @param teamQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/my/create")
    @AuthCheck(anyRole = {"admin","user","vip"})
    public BaseResponse<Page<TeamUserVO>> listCreateTeams(TeamQueryRequest teamQueryRequest,HttpServletRequest request) {
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 有点尬了这里.. 多查找了一次,以后再优化吧
        Long userId = userService.getLoginUser(request).getId();
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("createId", userId);
        List<Team> teamList = teamService.list(teamQueryWrapper);
        List<Long> idList = teamList.stream().map(Team::getId).collect(Collectors.toList());
        // 按照 id列表查询
        teamQueryRequest.setIdList(idList);
        List<TeamUserVO> resultList = teamService.listTeams(teamQueryRequest,request,true);
        Page<TeamUserVO> resultPage = new Page<>(teamQueryRequest.getCurrent(),teamQueryRequest.getPageSize());
        resultPage.setRecords(resultList);
        return ResultUtils.success(resultPage);
    }




}
