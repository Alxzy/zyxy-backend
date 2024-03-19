package com.zy.zyxy.service;

import com.zy.zyxy.model.dto.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-17 21:40
 * 用户插入单元测试，注意打包时要删掉或忽略，不然打一次包就插入一次
 */
@SpringBootTest
public class InsertUsersTest {

    @Resource
    private UserService userService;
    // 自定义线程配置
    private Executor excuterService = new ThreadPoolExecutor(20,1000,10000, TimeUnit.MINUTES,new ArrayBlockingQueue<>(10000));

    /**
     * 批量导入用户数据
     * 10W条数据 批量1W saveBatch用时: 8389ms
     */
    @Test
    public void doInsertUsers(){
//        final int INSERT_NUM = 1000;
        final int INSERT_NUM = 100000;

        // 计时工具
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        List<User> userList = new ArrayList<>();
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
            userList.add(user);
        }
        // 10W条数据 批量1W saveBatch用时: 8389ms
        userService.saveBatch(userList,10000);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

    /**
     * 并发导入用户数据
     * 10W条数据 并发导入20个线程 saveBatch用时: 2887ms
     *
     * 整个100W条数据测试一下 会发现前端查询时间中会卡顿
     */
    @Test
    public void doConcurrencyInsertUsers(){
//        final int INSERT_NUM = 1000;
        final int INSERT_NUM = 300000;
        // 记录值
        int j = 0;
        //批量插入数据的大小
        int batchSize = 15000;
        // 计时工具
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 任务列表
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        // 外层循环变量 组数
        for(int i = 0;i < 20;i++){
            List<User> userList = new ArrayList<>();
            while(true){
                j++;
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
                userList.add(user);
                if(j % batchSize == 0){
                    // 完成一批退出
                    break;
                }
            }
            // 异步执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("threadName: " + Thread.currentThread().getName());
                userService.saveBatch(userList, batchSize);
            }, excuterService);
            futureList.add(future);
        }
        // 等待所有线程执行完成
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
