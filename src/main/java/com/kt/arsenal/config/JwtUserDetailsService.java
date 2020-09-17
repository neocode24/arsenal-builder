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

import com.kt.arsenal.biz.login.domain.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

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
public class JwtUserDetailsService implements UserDetailsService {

    /**
     * LDAP 또는 DB 인증에 대한 사용자 인증 처리.
     * @param username
     * @param password
     * @return
     * @throws UsernameNotFoundException
     */
    public UserDetails loadUserByUsername(String username, String password) throws UsernameNotFoundException {

        // TODO : LDAP으로 ID,PW 요청하여 사용자 정보 조회 하는 처리에 대한 서비스 호출. SecurityUser 객체를 그대로 받는게 제일 좋음.
        return new SecurityUser(username, password, null, null);
    }


    /**
     * 사용자 ID만으로 조회하여 결과에서 얻은 PASSWORD를 가지고 SecurityUser 객체를 생성하는 방식은
     * 현재 KT LDAP에서 PASSWORD를 조회로 제공하지 않아 사용 불가.
     *
     * 따라서, username만 전달하는 기본 Method는 Token의 정상 여부만 확인하는 용도로 사용함.
     * LDAP 인증 이던, DB 인증 이던 TOKEN 확인차원에서 정상적인 사용자 임은 DB에서 확인함.
     *
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // TODO : username 으로 DB에서 사용자 상세 정보를 조회 하는 기능. SecurityUser 객체를 그대로 받는게 제일 좋음.
        return new SecurityUser(null, null);
    }

}
