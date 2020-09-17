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

import com.kt.arsenal.common.filter.JwtRequestFilter;
import com.kt.arsenal.common.utils.Sha256PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Arsenal-Dev
 *
 * @author 82022961
 * @version 1.0.0
 * @see
 * @since 30/09/2019
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private UserDetailsService jwtUserDetailsService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private CustomAuthentictionProvider customAuthentictionProvider;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

    // configure AuthenticationManager so that it knows from where to load
    // user for matching credentials
        auth.userDetailsService(jwtUserDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Sha256PasswordEncoder();     // SpringSecurity Default PasswordEncoder를 사용하지 않고, 64글자 기준의 SHA256 Encorder 사용함.
//        return new BCryptPasswordEncoder();   // Bcrypt Encoder를 사용하면, 암호화 문자열이 항상 "$2a$10$"로 시작함.
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    /**
     * 일반적인 AuthenticationProvider는 password 획득을 Spring Security 사상으로 제공하지 않고 있음.
     * KT LDAP 연동시에는 ID, PASSWORD가 필요함에 따라,
     * CustomAuthenticationProvder를 생성하여, 사용자가 입력한 ID, PASSWORD를 획득함.
     *
     * @param auth
     * @throws Exception
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(customAuthentictionProvider);
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {

        // We don't need CSRF
        httpSecurity.csrf().disable()
                // dont authenticate this particular request
                .authorizeRequests().antMatchers(
                        "/**"
                        ).permitAll()

                // all other requests need to be authenticated
                .anyRequest().authenticated().and()

                // make sure we use stateless session; session won't be used to
                // store user's state.
                .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint).and().sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        
        // multi login 시 IFRAME 사용 가능하도록
        //httpSecurity.headers().frameOptions().sameOrigin();
        httpSecurity.headers().frameOptions().disable();

        // Add a filter to validate the tokens with every request
        httpSecurity.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
