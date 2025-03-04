package com.zy.zyxy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zy.zyxy.model.dto.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户服务
 *
 * @author zy
 * 
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode,String phone,String email,Integer gender,String avatarUrl,String activeIds,String userName);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);


    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     * @param tagNameList 标签列表
     * @param loginUser 登录用户
     * @return 脱敏后的用户列表
     */
    List<User> searchUsersByTags(List<String> tagNameList,User loginUser);

    /**
     * 根据用户id修改用户信息
     * @param user
     * @param request
     * @return
     */
    boolean updateUser(User user, HttpServletRequest request);

    /**
     * 当前登录用户是否为管理员
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 根据当前用户匹配用户
     * @param loginUser
     * @param num
     * @return
     */
    Page<User> matchUser(User loginUser, Long num);
}
