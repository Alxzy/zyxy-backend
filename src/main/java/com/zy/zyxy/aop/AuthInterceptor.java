package com.zy.zyxy.aop;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.zy.zyxy.annotation.AuthCheck;
import com.zy.zyxy.exception.BusinessException;
import com.zy.zyxy.model.dto.User;
import com.zy.zyxy.model.enums.UserAuthEnum;
import com.zy.zyxy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.zy.zyxy.common.ErrorCode;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-03-22 21:31
 * 基于AOP的权限拦截器
 */
@Component
@Aspect
@Slf4j
public class AuthInterceptor {

    @Resource
    private UserService userService;


    /**
     * aop 实现拦截
     * (只要使用@Around接口就可以了)
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint , AuthCheck authCheck) throws Throwable {
        List<String> anyRole = Arrays.stream(authCheck.anyRole()).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 当前登录用户
        User user = userService.getLoginUser(request);
        // 用户未登录
        if(user == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 获取登录用户对应的权限
        Integer userRoleValue = user.getUserRole();
        UserAuthEnum userAuthEnum = UserAuthEnum.getValues(userRoleValue);
        if(userAuthEnum == null){ // 无对应的权限
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        String userRole = userAuthEnum.getText();
        // 拥有任意权限即通过
        if (CollectionUtils.isNotEmpty(anyRole)) {

            if (!anyRole.contains(userRole)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
        }
        // 必须有所有权限才通过
        if (StringUtils.isNotBlank(mustRole)) {
            if (!mustRole.equals(userRole)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
        }
        // 通过权限校验，放行
        return joinPoint.proceed();

    }
}
