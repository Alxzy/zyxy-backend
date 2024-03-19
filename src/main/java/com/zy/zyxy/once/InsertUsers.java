package com.zy.zyxy.once;
import java.util.Date;

import com.zy.zyxy.mapper.UserMapper;
import com.zy.zyxy.model.dto.User;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-17 21:23
 */
@Component
public class InsertUsers {

    @Resource
    private UserMapper userMapper;

    /**
     * 批量插入用户
     * 导入一次数据，不采用定时任务(稳一点)，使用单元测试
     *
     */
//    @Scheduled(initialDelay = 5000,fixedRate = Long.MAX_VALUE)
    public void doInsertUsers(){
//        final int INSERT_NUM = 10000000;  别玩这么大
        final int INSERT_NUM = 1000;
        // 计时工具
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for(int i = 0;i < INSERT_NUM;i++){
            User user = new User();
            user.setUsername("fakeUser");
            user.setUserAccount("fakezyzy");
            user.setAvatarUrl("https://img0.baidu.com/it/u=1150065970,2999223123&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500");
            user.setGender(1);
            user.setUserPassword("12345678");
            user.setPhone("123321");
            user.setEmail("123321@qq.com");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("1111111");
            user.setTags("[]");
            user.setProfile("hello！！！");
            userMapper.insert(user);
        }
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

    // 不加载 spring 包报错的
//    public static void main(String[] args) {
//        new InsertUsers().doInsertUsers();
//    }
}
