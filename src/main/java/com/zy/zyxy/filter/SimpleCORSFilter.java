package com.zy.zyxy.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Administrator
 * @version 1.0
 * @date 2024-04-11 2:06
 * 跨域过滤器
 */
@Slf4j
//@Component
public class SimpleCORSFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        log.info("我被注入了！");
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        response.setHeader("Access-Control-Allow-Origin","https://zyxy.ai-haitham-gsim.icu");
        response.setHeader("Access-Control-Allow-Methods","POST, GET, OPTIONS, DELETE, PUT");
        response.setHeader("Access-Control-Allow-Headers","access-control-allow-origin, authority, content-type, version-info, X-Requested-With");
        response.setHeader("Access-Control-Max-Age","3600");
        response.setHeader("Access-Control-Allow-Credentials","true");
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if("OPTION".equals(request.getMethod())){
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }


        filterChain.doFilter(servletRequest,servletResponse);



    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
