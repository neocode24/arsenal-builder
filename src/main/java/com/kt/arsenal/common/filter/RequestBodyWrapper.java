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

import com.nhncorp.lucy.security.xss.XssFilter;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;


/**
 * RequestBody JSON XSS Filter Wrapper
 * @author 91218672
 * @since 2018.12.11.
 * @version 1.0.0
 * @see
 */
public class RequestBodyWrapper extends HttpServletRequestWrapper {

    //private final byte[] bytes;
    private byte[] bytes;
    

    public RequestBodyWrapper(HttpServletRequest request) throws IOException {
        super(request);
        XssFilter filter = XssFilter.getInstance("lucy-xss-superset-sax.xml", true);
        bytes = filter.doFilter(getRequestBody(request)).getBytes();
    }
    
    public String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder requestBody = new StringBuilder();
        
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
            char[] charBuffer = new char[128];
            int bytesRead = -1;
            while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                requestBody.append(charBuffer, 0, bytesRead);
            }
        }
        
        return requestBody.toString();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        return new ServletImpl(bis);
    }

    class ServletImpl extends ServletInputStream {
        
        //private final InputStream is;
        private InputStream is;

        public ServletImpl(InputStream bis) {
            is = bis;
        }

        @Override
        public int read() throws IOException {
            return is.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return is.read(b);
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            // setReadListener
        }
        
    }
}