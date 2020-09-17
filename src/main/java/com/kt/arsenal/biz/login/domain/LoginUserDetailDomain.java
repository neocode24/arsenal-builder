package com.kt.arsenal.biz.login.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * User Domain Detail Object
 * @author neocode24
 * @since 2019.10.07.
 * @version 1.0.0
 */
@Data
public class LoginUserDetailDomain implements Serializable {

    /**
     * 사용자ID
     */
    private String username;

    /**
     * 사용자 비밀번호
     */
    private String password;

    /**
     * 사용자명
     */
    private String name;

    /**
     * 최근접속IP
     */
    private String accessIp;

    /**
     * 소속회사
     */
    private String companyNm;

    /**
     * 부서명
     */
    private String deptNm;

    /**
     * 이메일
     */
    private String email;

    /**
     * 연락처
     */
    private String telNo;

    /**
     * 모바일 단말 MAC Address
     */
    private String macAddr;

    /**
     * 계정 상태 코드 : 1 재직중
     */
    private String statusCd;
}
