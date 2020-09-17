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

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;


/**
 * RequestBody JSON XSS Filter
 * @author 91218672
 * @since 2018.12.11.
 * @version 1.0.0
 * @see
 */
public class RequestBodyFilter implements Filter {
    
    @Override
    public void destroy() {
        // filter destroy
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        
        boolean isAjax = isAjaxRequest((HttpServletRequest) request);
        boolean isJson = isJsonContentType(request);
        
        try {
            if (isAjax && isJson) {
                RequestBodyWrapper reqWrapper = new RequestBodyWrapper((HttpServletRequest) request);
                chain.doFilter(reqWrapper, response);
            } else {
                chain.doFilter(request, response);
            }
        } catch (IOException e) {
            chain.doFilter(request, response);    
        }
        
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // filter init
    }
    
    private boolean isJsonContentType(ServletRequest request) {
        String contentType = request.getContentType();
        return (contentType != null && contentType.toLowerCase().contains("json"));
    }
    
    private boolean isAjaxRequest(HttpServletRequest req){
        return "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
    }

}
