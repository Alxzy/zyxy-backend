package com.zy.zyxy.service;
import java.util.Date;

import com.zy.zyxy.model.dto.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-18 22:09
 */

@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;

    @Test
    public void test(){
        ValueOperations valueOperations = redisTemplate.opsForValue();

        // 增
        valueOperations.set("111","111");
        valueOperations.set("22",2);
        valueOperations.set("33333",3.0);
        User user = new User();
        user.setId(3L);
        user.setUsername("44444");
        valueOperations.set("user",user);
        // 查
        Object o = valueOperations.get("111");
        Assertions.assertTrue("111".equals(o));
        o = valueOperations.get("22");
        Assertions.assertTrue(2 == (Integer) o);
        o = valueOperations.get("33333");
        Assertions.assertTrue(3.0 == (Double) o);
        o = valueOperations.get("user");
        System.out.println(o);
        // 删

    }

}
