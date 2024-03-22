package com.zy.zyxy.aop;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.stereotype.Component;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-22 21:31
 * 基于AOP的全局请求响应日志拦截器
 */
@Component
@Aspect
@Slf4j
public class LogInterceptor {
    /**
     * aop 实现拦截
     * (只要使用@Around接口就可以了)
     */
    @Around("execution(* com.zy.zyxy.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        // 1.请求处理
        // 1.1 开始计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 1.2 获取请求路径
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();// RequestContextHolder是一个请求绑定到线程的类 原理见 SpringMVC
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 1.3 生成请求唯一 id
        String requestId = UUID.randomUUID().toString();
        String url = httpServletRequest.getRequestURI(); // 相对路径 不完整的url地址
        // 1.4 获取请求参数
        Object[] args = point.getArgs();
        String reqParam = "[" + StringUtils.join(args, ", ") + "]";
        // 1.5 输出请求日志
        log.info("request start，id: {}, path: {}, ip: {}, params: {}", requestId, url,
                httpServletRequest.getRemoteHost(), reqParam);
        // 2.执行原方法
        Object result = point.proceed();
        // 3.响应
        // 3.1 停止计时,计算时间
        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();
        // 3.2 输出响应日志
        log.info("request end, id: {}, cost: {}ms", requestId, totalTimeMillis);
        return result;

    }
}
