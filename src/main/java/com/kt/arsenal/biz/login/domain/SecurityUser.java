package com.kt.arsenal.biz.login.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring Security User Detail Object
 * @author neocode24
 * @since 2019.10.07.
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class SecurityUser extends User {

    /**
     * 사용자ID
     */
    private String username;

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


    /**
     * Token 확인시 조회하는 사용자 상세 정보.
     * @param loginUserDetailDomain
     * @param authorities
     */
    public SecurityUser(LoginUserDetailDomain loginUserDetailDomain, List<GrantedAuthority> authorities) {
        super(loginUserDetailDomain.getUsername(), Optional.ofNullable(loginUserDetailDomain.getPassword()).orElse("[Not send password]"), authorities);

        this.username = loginUserDetailDomain.getUsername();

        this.name = loginUserDetailDomain.getName();
        this.accessIp = loginUserDetailDomain.getAccessIp();
        this.companyNm = loginUserDetailDomain.getCompanyNm();
        this.deptNm = loginUserDetailDomain.getDeptNm();
        this.email = loginUserDetailDomain.getEmail();
        this.telNo = loginUserDetailDomain.getTelNo();
        this.macAddr = loginUserDetailDomain.getMacAddr();
        this.statusCd = loginUserDetailDomain.getStatusCd();
    }


    /**
     * Login 시점(Ldap, DB)에 생성해서 Token으로 만들기 위한 생성자
     * @param userId
     * @param password
     * @param loginUserInfo
     * @param authorities
     */
    public SecurityUser(String userId, String password, Map<String, String>loginUserInfo, List<GrantedAuthority> authorities) {
        super(userId, password, authorities);

        this.username = userId;

        this.name = loginUserInfo.get("userName");
        this.deptNm = loginUserInfo.get("deptName");
        this.companyNm = loginUserInfo.get("companyName");
        this.telNo = loginUserInfo.get("mobile");
        this.email = loginUserInfo.get("email");

        this.accessIp = loginUserInfo.get("accessIp");
        this.macAddr = loginUserInfo.get("macAddr");
        this.statusCd = loginUserInfo.get("statusCd");

    }

}
