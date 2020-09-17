/*
 * Arsenal-Platform version 1.0
 * Copyright ⓒ 2019 kt corp. All rights reserved.
 * This is a proprietary software of kt corp, and you may not use this file except in
 * compliance with license agreement with kt corp. Any redistribution or use of this
 * software, with or without modification shall be strictly prohibited without prior written
 * approval of kt corp, and the copyright notice above does not evidence any actual or
 * intended publication of such software.
 */
package com.kt.arsenal.common.constant;


/**
 * 공통 코드 조회 상수 정의
 * @author 91221660
 * @since 2019.02.13.
 * @version 1.0.0
 */
public class CodeConstUtil {

    /**
     * Cluster Authorization Level : SU[Super User]
     */
    public static final String SUPER_USER = "SU";
    
    /**
     * Cluster Authorization Level : CA[Cluster Admin] 
     */
    public static final String CLUSTER_ADMIN = "CA";
    
    
    /**
     * Cluster Authorization Level : CU[Cluster User]
     */
    public static final String CLUSTER_USER = "CU";
    
    
    /**
     * Namespace Authorization Level : NA[Namespace Admin]
     */
    public static final String NAMESPACE_ADMIN = "NA";
    
    
    /**
     * Namespace Authorization Level : NU[Namespace User]
     */
    public static final String NAMESPACE_USER = "NU";

    /**
     * 플랫폼 그룹코드 : comn_group_cd [PLATFORM_VERSION]
     */
    public static final String PLATFORM_VERSION = "PLATFORM_VERSION";
    
    /**
     * 공지유형코드 : comn_group_cd [NOTICE_TYPE]
     */
    public static final String NOTICE_TYPE = "NOTICE_TYPE";

    /**
     * Arsenal LOCAL 환경 구분 상수
     */
    public static final String ENVIRONMENT_ACTIVE_PROFILES_LOCAL = "local";

    /**
     * Arsenal DEV 환경 구분 상수
     */
    public static final String ENVIRONMENT_ACTIVE_PROFILES_DEV = "dev";


    /**
     * Arsenal DEV-PRD 환경 구분 상수
     */
    public static final String ENVIRONMENT_ACTIVE_PROFILES_DEV_PRD = "dev-prd";


    /**
     * Arsenal PRD 환경 구분 상수
     */
    public static final String ENVIRONMENT_ACTIVE_PROFILES_PRD = "prd";


    /**
     * Arsenal EPC 환경 구분 상수
     */
    public static final String ENVIRONMENT_ACTIVE_PROFILES_EPC = "epc";



    /**
     * 클러스터(Namespace) 공통 URL 구분(Type) 코드 : comn_group_cd [COMN_URL_TYPE]
     */
    public static final String COMN_URL_TYPE = "COMN_URL_TYPE";


    /**
     * 사용자 계정상태 1 : 사용
     */
    public static final String ACCOUNT_STATUS_CD_ENABLED = "1";

    /**
     * 사용자 계정상태 2 : 휴면
     */
    public static final String ACCOUNT_STATUS_CD_INACTIVE = "2";

    /**
     * 사용자 계정상태 3 : 미사용
     */
    public static final String ACCOUNT_STATUS_CD_DISABLED = "3";

    /**
     * 사용자 계정상태 4 : 비밀번호 초기화 요청 상태
     */
    public static final String ACCOUNT_STATUS_CD_PASSWORD_RESET = "4";

    /**
     * 초기 비밀번호 상수
     */
    public static final String ACCOUNT_INITIAL_PASSWORD = "new1234@";

    /**
     * 자동화 구성 상태 0 : 요청 접수
     */
    public static final String AUTO_ENV_SERVICE_STATUS_SCHEDULED = "scheduled";

    /**
     * 자동화 구성 상태 1 : 시작
     */
    public static final String AUTO_ENV_SERVICE_STATUS_STARTED = "started";

    /**
     * 자동화 구성 상태 2 : 정상 종료
     */
    public static final String AUTO_ENV_SERVICE_STATUS_DONE = "done";

    /**
     * 자동화 구성 상태 3 : 오류 종료
     */
    public static final String AUTO_ENV_SERVICE_STATUS_FAILED = "failed";
    
    /**
     * Favorite link 구분(Type) 코드 : comn_group_cd [FAVORITE_LINK_TYPE]
     */
    public static final String FAVORITE_LINK_TYPE = "FAVORITE_LINK_TYPE";
    
    
    private CodeConstUtil() {
        throw new IllegalStateException("Non-instantiable Constant Class");
    }
    
}
