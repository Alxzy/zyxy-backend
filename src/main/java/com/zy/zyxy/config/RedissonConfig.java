package com.zy.zyxy.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-20 20:38
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    // 主机IP
    String host;
    // 端口号
    String port;
    // 数据库号 这里不使用是因为 原来那个库 是用于缓存数据的
    // 分布式锁 属于另一个功能,因此这里分到另外一个库中
//    private String database;


    @Bean
    public RedissonClient redissonClient(){
        // 1. 创建配置
        Config config = new Config();
        // 地址最好不要写死
        String redisAddress = String.format("redis://%s:%s", host, port);
        // 地址 密码 库 没有开集群就使用单机
        config.useSingleServer().setAddress(redisAddress).setDatabase(3);;

        // 2. 创建 Redisson实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }

}
