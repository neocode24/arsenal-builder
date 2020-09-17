/*
 * Arsenal-Platform version 1.0
 * Copyright ⓒ 2019 kt corp. All rights reserved.
 * This is a proprietary software of kt corp, and you may not use this file except in
 * compliance with license agreement with kt corp. Any redistribution or use of this
 * software, with or without modification shall be strictly prohibited without prior written
 * approval of kt corp, and the copyright notice above does not evidence any actual or
 * intended publication of such software.
 */
package com.kt.arsenal.common.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * 메서드 필터(GET, POST 메서드만 허용)
 * @author 91218672
 * @since 2018.12.11.
 * @version 1.0.0
 * @see
 */
@Slf4j
@Component
public class HttpMethodFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String httpMethod = request.getMethod().toUpperCase();
        
        if ("GET".equals(httpMethod) || "POST".equals(httpMethod)) {
            if (!"/storage.html".equals(request.getRequestURI())) {
                response.setHeader("X-Frame-Options", "DENY");
            }
            filterChain.doFilter(request, response);
        } else {
            log.warn("NOT ALLOW {} Method!!!!!!!", httpMethod);
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            
        }
    
    }

}
