package com.zy.zyxy.contant;

/**
 * 用户常量
 *
 * @author zy
 * 
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "userLoginState";

    /**
     * 用户个性化推荐 Redis缓存键
     */
    String USER_RECOMMEND_KEY = "zyxy:user:recommed:%s";

    //  ------- 权限 --------

    /**
     * 默认权限
     */
    int DEFAULT_ROLE = 0;


    /**
     * 管理员权限
     */
    int ADMIN_ROLE = 1;

}
