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

import com.kt.arsenal.biz.portal.service.PortalCallerService;
import com.kt.arsenal.common.exception.CustomArsenalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;

/**
 * Arsenal-Dev
 *
 * @author 82022961
 * @version 1.0.0
 * @see
 * @since 30/09/2019
 */

@Slf4j
@Component
public class JwtRequestFilter extends OncePerRequestFilter {



    @Autowired
    private PortalCallerService portalCallerService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        // Bearer Token이 있는 경우 Portal에서 Token 확인 함.
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {

            Enumeration<String> headerNames = request.getHeaderNames();
            HttpHeaders httpHeaders = new HttpHeaders();

            while (headerNames.hasMoreElements()) {
                String key = headerNames.nextElement();
                String value = request.getHeader(key);
                httpHeaders.set(key, value);
            }


            try {
                UserDetails userDetails = portalCallerService.isValidateRequestToken(httpHeaders);

                // 사용자 Token이 정상이면 생성된 UserDetail 정보로 부터 Spring Session에 정상적인 권한을 부여 함.
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // After setting the Authentication in the context, we specify
                // that the current user is authenticated. So it passes the
                // Spring Security Configurations successfully.
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);


                chain.doFilter(request, response);
            } catch (CustomArsenalException e) {
                throw e;

            } catch (Exception e) {
                log.error("Token 정보가 유효하지 않습니다. Portal Service에서 인증되지 않았습니다.");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }






        }
        // Token이 없는 경우 일부 예외 URI만 확인하여 허용 함.
        else {

            // 호출 예외 URI
            if ( request.getRequestURI().indexOf("/actuator") > -1
                    ||
                 request.getRequestURI().indexOf("/builder") > -1
            ) {
                chain.doFilter(request, response);
            }
            else {
                log.error("JWT Token with bearer 없이 사이트를 요청하였습니다. request uri:[{}]", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }
    }
}
