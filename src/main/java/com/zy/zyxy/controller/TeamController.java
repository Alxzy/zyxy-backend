package com.zy.zyxy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zy.zyxy.annotation.AuthCheck;
import com.zy.zyxy.common.BaseResponse;
import com.zy.zyxy.common.ErrorCode;
import com.zy.zyxy.common.ResultUtils;
import com.zy.zyxy.exception.BusinessException;
import com.zy.zyxy.model.domain.request.TeamAddRequest;
import com.zy.zyxy.model.domain.request.UserLoginRequest;
import com.zy.zyxy.model.domain.request.UserRegisterRequest;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.service.TeamService;
import com.zy.zyxy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zy.zyxy.contant.UserConstant.USER_LOGIN_STATE;
import static com.zy.zyxy.contant.UserConstant.USER_RECOMMEND_KEY;

/**
 * 用户接口
 *
 * @author zy
 * 
 */
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://localhost:5173/"},allowCredentials = "true")
@Slf4j
public class TeamController {

    @Resource
    private TeamService teamService;

   @PostMapping("/add")
   @AuthCheck(mustRole = "admin")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest,HttpServletRequest request){


       return null;
   }








}
