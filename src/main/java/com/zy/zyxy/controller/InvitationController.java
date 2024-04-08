package com.zy.zyxy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zy.zyxy.annotation.AuthCheck;
import com.zy.zyxy.common.BaseResponse;
import com.zy.zyxy.common.DeleteRequest;
import com.zy.zyxy.common.ErrorCode;
import com.zy.zyxy.common.ResultUtils;
import com.zy.zyxy.exception.BusinessException;
import com.zy.zyxy.model.domain.request.InviteRequest;
import com.zy.zyxy.model.domain.request.TagQueryRequest;
import com.zy.zyxy.model.dto.Invitation;
import com.zy.zyxy.model.dto.Tag;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.model.vo.TagVO;
import com.zy.zyxy.model.vo.TeamUserVO;
import com.zy.zyxy.service.InvitationService;
import com.zy.zyxy.service.TagService;
import com.zy.zyxy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zy.zyxy.contant.TagConstant.TAG_CATEGORY_KEY;
import static com.zy.zyxy.contant.TagConstant.TAG_LIST_KEY;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-04-02 20:47
 * 邀请接口
 */
@RestController
@RequestMapping("/invitation")
@CrossOrigin(origins = {"http://localhost:5173/"}, allowCredentials = "true")
@Slf4j
public class InvitationController {
    @Resource
    private UserService userService;

    @Resource
    private TagService tagService;

    @Resource
    private InvitationService invitationService;


    @PostMapping("/invite")
    @AuthCheck(anyRole = {"admin", "user", "vip"})
    public BaseResponse<Boolean> invite(@RequestBody InviteRequest inviteRequest, HttpServletRequest request) {
        // 校验参数
        if (inviteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = invitationService.inviteRequest(inviteRequest,loginUser);


        return ResultUtils.success(result);
    }



    /**
     * 根据id 获取邀请
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(anyRole = {"admin"})
    public BaseResponse<Invitation> getInvitationById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Invitation invitation = invitationService.getById(id);
        if (invitation == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(invitation);
    }


    /**
     * 获取我的邀请
     *
     * @param request
     * @return 邀请我的队伍列表
     */
    @GetMapping("/my")
    @AuthCheck(anyRole = {"admin", "user", "vip"})
    public BaseResponse<List<TeamUserVO>> listMyInvitations(HttpServletRequest request) {

        List<TeamUserVO> resultList = invitationService.getMyInvitation(request);

        return ResultUtils.success(resultList);
    }

    @PostMapping("/ignore/{teamId}")
    @AuthCheck(anyRole = {"admin", "user", "vip"})
    public BaseResponse<Boolean> ignore(@PathVariable("teamId") long teamId, HttpServletRequest request) {
        // 校验参数
        if ( teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍不存在");
        }
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getId();
        // 删除数据
        QueryWrapper<Invitation> invitationQueryWrapper = new QueryWrapper<>();
        invitationQueryWrapper.eq("teamId",teamId);
        invitationQueryWrapper.eq("userId",loginUserId);
        boolean result = invitationService.remove(invitationQueryWrapper);
        if(!result){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"拒绝失败");
        }
        return ResultUtils.success(result);
    }

    @PostMapping("/accept/{teamId}")
    @AuthCheck(anyRole = {"admin", "user", "vip"})
    public BaseResponse<Boolean> accept(@PathVariable("teamId") long teamId, HttpServletRequest request) {
        // 校验参数
        if (teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍不存在");
        }
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getId();

        boolean result = invitationService.agree(loginUserId,teamId);
        if(!result){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"接受失败,加入队伍错误");
        }
        return ResultUtils.success(result);
    }




}
