package com.zy.zyxy.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zy.zyxy.annotation.AuthCheck;
import com.zy.zyxy.common.BaseResponse;
import com.zy.zyxy.common.ErrorCode;
import com.zy.zyxy.common.ResultUtils;
import com.zy.zyxy.exception.BusinessException;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.model.domain.request.UserLoginRequest;
import com.zy.zyxy.model.domain.request.UserRegisterRequest;
import com.zy.zyxy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zy.zyxy.contant.UserConstant.*;

/**
 * 用户接口
 *
 * @author zy
 * 
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://localhost:5173/"},allowCredentials = "true")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 校验
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前用户
     *
     * @param request
     * @return
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();
        // TODO 校验用户是否合法
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    // https://zy.icu/

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "缺少管理员权限");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }



    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 根据标签列表搜索用户列表
     * @param tagsList 标签列表
     * @return
     */
    @GetMapping("/search/tags")
    private BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagsList){
        //校验
        if(CollectionUtils.isEmpty(tagsList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        // 调用方法
        List<User> users = userService.searchUsersByTags(tagsList);

        return ResultUtils.success(users);
    }

    /**
     * 根据登录用户个性化推荐
     * @param current
     * @param pagesize
     * @param request
     * @return
     */
    @GetMapping("/recommend")
    private BaseResponse<Page<User>> recommendUsers(Long current,Long pagesize,HttpServletRequest request){
        // todo 个性化推荐算法
        // 获取登录状态 个性化 推荐
        // 1.权限校验
        User loginUser = userService.getLoginUser(request);
        // 生成 对应用户的 key
        String userRedisKey = String.format(USER_RECOMMEND_KEY, loginUser.getId());


        // 2.从缓存中获取数据
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Page<User> userPage = ( Page<User>) valueOperations.get(userRedisKey);
        // 缓存不为空 直接返回
        if(userPage != null){
            return ResultUtils.success(userPage);
        }
        // 3.从数据库中查询

        // 分页
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        List<User> userList = userService.list(queryWrapper);

        if(current == null){
            current = 1l;
        }
        if(pagesize == null){
            pagesize = 15l;
        }
        userPage = userService.page(new Page<>(current, pagesize), queryWrapper);
        // 修改为安全用户
        List<User> list = userPage.getRecords().stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        userPage.setRecords(list);
        // 加载到Redis中
        // 设置过期时间为30s
        try {
            valueOperations.set(userRedisKey,userPage,30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis set key error : " + userRedisKey,e);
        }
        return ResultUtils.success(userPage);
    }

    @PutMapping ("/update")
    @AuthCheck(anyRole = {"admin","user","vip"})
    public BaseResponse<Boolean> updateUserById(@RequestBody User user, HttpServletRequest request) {
        // 参数校验
        if(user == null || user.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.updateUser(user,request);
        return ResultUtils.success(result);
    }

    @GetMapping("/match")
    @AuthCheck(anyRole = {"admin","user","vip"})
    public BaseResponse<Page<User>> matchUser(Long num,HttpServletRequest request) {
        // 校验参数是否合理
        if(num == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if(num <= 0 || num > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"匹配用户过多");
        }
        // 取出登录用户
        User loginUser = userService.getLoginUser(request);
        Page<User> userList = userService.matchUser(loginUser,num);
        return ResultUtils.success(userList);
    }








}
