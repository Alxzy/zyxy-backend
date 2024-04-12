package com.zy.zyxy.service;

import com.zy.zyxy.model.dto.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
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
        ArrayList<String> planetList = new ArrayList<>();
        planetList.add("阅读");
        planetList.add("打游戏");
        planetList.add("美食");
        planetList.add("编程学习");
        planetList.add("旅行");
        planetList.add("运动");
        planetList.add("音乐");
        planetList.add("演出");
        for(int i = 0;i < INSERT_NUM;i++){
            User user = new User();
            user.setUsername("fakeUser");
            user.setUserAccount("fakezyzy" + i);
            user.setAvatarUrl("https://img1.baidu.com/it/u=3333815031,2954843873&fm=253&fmt=auto&app=138&f=JPEG?w=507&h=500");
            user.setGender(1);
            user.setUserPassword("12345678");
            user.setPhone("123321");
            user.setEmail("123321@qq.com");
            user.setUserStatus(0);
            user.setUserRole(0);
            String planetCode = planetList.get(i % 8);
            user.setPlanetCode(planetCode);
            user.setTags("[" + planetCode  + "]");
            user.setProfile("你好，我是练习时长两年半的个人练习生，我的爱好是 " + planetCode);
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
        final int INSERT_NUM = 50000;
        // 记录值
        int j = 0;
        //批量插入数据的大小
        int batchSize = 2500;
        // 计时工具
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 任务列表
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        HashMap<String,String> planetMap = new HashMap<>();
        ArrayList<String> planetCodeList = new ArrayList<>();
        planetCodeList.add("阅读");
        planetCodeList.add("打游戏");
        planetCodeList.add("美食");
        planetCodeList.add("编程学习");
        planetCodeList.add("旅行");
        planetCodeList.add("运动");
        planetCodeList.add("音乐");
        planetCodeList.add("演出");
        planetMap.put("阅读","[\"阅读\"]");
        planetMap.put("打游戏","[\"打游戏\"]");
        planetMap.put("美食","[\"美食\"]");
        planetMap.put("编程学习","[\"编程学习\"]");
        planetMap.put("旅行","[\"旅行\"]");
        planetMap.put("运动","[\"运动\"]");
        planetMap.put("音乐","[\"音乐\"]");
        planetMap.put("演出","[\"演出\"]");
        // 外层循环变量 组数
        for(int i = 0;i < 20;i++){
            List<User> userList = new ArrayList<>();
            while(true){
                j++;
                User user = new User();
                user.setUsername("fakeUser");
                user.setUserAccount("fakeuser");
                user.setAvatarUrl("https://img1.baidu.com/it/u=3333815031,2954843873&fm=253&fmt=auto&app=138&f=JPEG?w=507&h=500");
                user.setGender(1);
                user.setUserPassword("12345678");
                user.setPhone("123321");
                user.setEmail("123321@qq.com");
                user.setUserStatus(0);
                user.setUserRole(0);
                String planetCode = planetCodeList.get(j % 8);
                user.setPlanetCode(planetCode);
                user.setTags(planetMap.get(planetCode));
                user.setProfile("你好，我是练习时长两年半的个人练习生，我的爱好是 " + planetCode);
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
