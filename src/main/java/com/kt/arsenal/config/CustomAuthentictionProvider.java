/*
 * Arsenal-Platform version 1.0
 * Copyright ⓒ 2019 kt corp. All rights reserved.
 * This is a proprietary software of kt corp, and you may not use this file except in
 * compliance with license agreement with kt corp. Any redistribution or use of this
 * software, with or without modification shall be strictly prohibited without prior written
 * approval of kt corp, and the copyright notice above does not evidence any actual or
 * intended publication of such software.
 */
package com.kt.arsenal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Arsenal-Dev
 * 일반적인 AuthenticationProvider는 password 획득을 Spring Security 사상으로 제공하지 않고 있음.
 * KT LDAP 연동시에는 ID, PASSWORD가 필요함에 따라,
 * CustomAuthenticationProvder를 생성하여, 사용자가 입력한 ID, PASSWORD를 획득함.
 *
 * @author 82022961
 * @version 1.0.0
 * @see
 * @since 30/09/2019
 */
@Component
public class CustomAuthentictionProvider implements AuthenticationProvider {

    @Autowired
    private JwtUserDetailsService userDetailsService;


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String name = authentication.getName();
        String password = authentication.getCredentials().toString();

        return new UsernamePasswordAuthenticationToken(userDetailsService.loadUserByUsername(name, password), password, null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
