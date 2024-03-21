package com.zy.zyxy.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.zy.zyxy.contant.RedisConstant.PRECACHEJOB_KEY;
import static com.zy.zyxy.contant.UserConstant.USER_RECOMMEND_KEY;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-19 20:48
 */
@Component
@Slf4j
public class PreCacheJob {
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    // 重点用户 不建议这么写死
    // todo 后续优化为动态可控的
    private List<Long> mainUserList = Arrays.asList(1L, 6L, 7L, 8L, 9L, 10L);

    /**
     * 每天执行,预热推荐用户
     */
    @Scheduled(cron = "0 6 21 * * *")
    public void doCacheRecommendUser() {
        // 分布式锁
        RLock lock = redissonClient.getLock(PRECACHEJOB_KEY);
        // 获取到锁的线程进行加载数据
        try {
            // 因为每次只需要一个线程执行加载数据到缓存即可,其他线程无需等待,抢不到就别抢了
            /** 看源码的参数列表: 等待时间 释放时间 和 单位
             * long waitTime, long leaseTime, TimeUnit unit
             */
            if (lock.tryLock(0, 30000, TimeUnit.MILLISECONDS)) {
                System.out.println("getLock:" + Thread.currentThread().getName());
                // 遍历用户
                for (Long userId : mainUserList) {
                    // 查数据库 预热缓存
                    // todo 按照用户个性化推荐
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id",userId);
                    Page<User> userPage = userService.page(new Page<>(1l, 15l), queryWrapper);
                    // 生成 对应用户的 key
                    String userRedisKey = String.format(USER_RECOMMEND_KEY, userId);
                    // 预热缓存
                    // todo 设计合理的过期时间
                    ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                    try {
                        // 写缓存
                        valueOperations.set(userRedisKey, userPage, 60, TimeUnit.MINUTES);
                    } catch (Exception e) {
                        log.error("redis set key error : " + userRedisKey, e);
                    }
                }
            }

            // 这里释放锁有问题
//            if(lock.isHeldByCurrentThread()){
//                lock.unlock();
//            }
        } catch (InterruptedException e) {
            log.error("precacheJob fail");
        } finally {// 这里释放锁是对的
            /** 保证只能释放自己的锁
             * public boolean isHeldByCurrentThread() { //这个方法唯一标识线程
             *         return this.isHeldByThread(Thread.currentThread().getId());
             *     }
             */
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                System.out.println("unLock:" + Thread.currentThread().getName());
            }
        }


    }
}
