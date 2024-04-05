package com.zy.zyxy.contant;

/**
 * 队伍常量
 *
 * @author zy
 * 
 */
public interface TeamConstant {

    /**
     * 用户最大队伍数量
     */
    int DEFAULT_MAX_TEAM_NUM = 5;

    /**
     * 添加队伍 队伍锁
     */
    String USER_JOIN_TEAM_TEAM_KEY = "zyxy:join:team:team:%s";

}
