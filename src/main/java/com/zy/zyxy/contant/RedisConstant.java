package com.zy.zyxy.contant;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-20 21:54
 * Redis键的前缀常量
 */
public interface RedisConstant {

    /**
     * 用户个性化推荐 Redis缓存键
     */
    String PRECACHEJOB_KEY = "zyxy:precachejob:lock";
}
