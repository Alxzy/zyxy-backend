package com.zy.zyxy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类
 *
 */
@SpringBootApplication
@MapperScan("com.zy.zyxy.mapper")
@EnableScheduling
public class ZyxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZyxyApplication.class, args);
    }

}
