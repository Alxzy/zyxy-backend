package com.zy.zyxy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zy.zyxy.annotation.AuthCheck;
import com.zy.zyxy.common.BaseResponse;
import com.zy.zyxy.common.DeleteRequest;
import com.zy.zyxy.common.ErrorCode;
import com.zy.zyxy.common.ResultUtils;
import com.zy.zyxy.exception.BusinessException;
import com.zy.zyxy.model.domain.request.TeamAddRequest;
import com.zy.zyxy.model.domain.request.TeamJoinRequest;
import com.zy.zyxy.model.domain.request.TeamQueryRequest;
import com.zy.zyxy.model.domain.request.TeamUpdateRequest;
import com.zy.zyxy.model.dto.Team;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.model.enums.TeamStatusEnum;
import com.zy.zyxy.model.vo.TeamUserVO;
import com.zy.zyxy.service.TeamService;
import com.zy.zyxy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;


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

    @DeleteMapping("/delete")
    @AuthCheck(anyRole = {"admin","user","vip"})
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest) {
        // 参数校验
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = deleteRequest.getId();
        // 校验id
        if(teamId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 校验是否能查询到要删除的数据

        boolean result = teamService.removeById(teamId);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(result);
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
        List<TeamUserVO> resultList = teamService.listTeams(teamQueryRequest,request);
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


}
