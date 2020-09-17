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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.math.NumberUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

/**
 * 공통 상수 정의
 * @author 91218672
 * @since 2018.12.01.
 * @version 1.0.0
 * @see
 */
public class ConstUtil {
    
    /** LOGIN_SUCCESS : Y **/
    public static final String LOGIN_SUCCESS = "Y";
    
    /** LOGIN_FAILURE : N **/
    public static final String LOGIN_FAILURE = "N";
    
    /** LOGIN_SESSION_INTERVAL : 1 hour (60 * 60) **/
    public static final Integer ONE_HOUR = 3600;
    
    /** LOGIN_SESSION_INTERVAL : 30 minutes (60 * 30) **/
    public static final Integer HALF_HOUR = 1800;
    
    /** LOGIN FAIL REASON : 비밀번호 인증실패 5회 초과 **/
    public static final String AUTH_LIMIT_OVER = "비밀번호 인증 실패 5회 초과";
    
    /** LOGIN AUTH_LIMIT_COUNT : 비밀번호 인증 5회 한도 **/
    public static final Integer AUTH_LIMIT_COUNT = 5;

    /** LOGIN FAIL REASON : 비밀번호 인증 실패 **/
    public static final String CREDNTIALS_EXPIRED = "비밀번호 만료";

    /** LOGIN FAIL REASON : 비밀번호 인증 실패 **/
    public static final String CREDNTIALS_MISMATCH = "비밀번호 인증 실패";

    /** LOGIN FAIL REASON : 아이디 휴면 상태 **/
    public static final String CREDNTIALS_INACTIVE = "아이디 휴면 상태 로그인 실패";
    
    /** LOGIN FAIL REASON : 아이디 조회 실패 **/
    public static final String NOT_ENROLL_USER = "등록된 사용자 아님";
    
    /** LOGIN FAIL REASON : 로그인 차단 강제 초기화 **/
    public static final String RESET_BLOCK_LOGIN = "로그인 차단 강제 초기화";
    
    /** ISO Date Format [yyyy-MM-dd'T'HH:mm:ss.SSSZ] **/
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    
    /** Mysql Date Format [yyyy-MM-dd HH:mm:ss] **/
    public static final String MYSQL_DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
    
    /** Log Date Format [yyyyMMddHHmmss] **/
    public static final String LOG_DATE_FORMAT = "yyyyMMddHHmmss";
    
    /** No Pagination **/
    public static final int NO_PAGINATION = NumberUtils.INTEGER_ZERO;
    
    /** Redirect prefix : redirect: **/
    public static final String REDIRECT_PREFIX = "redirect:";
    
    /** Redirect login url : redirect:/login **/
    public static final String REDIRECT_LOGIN = "redirect:/login";
    
    /** Redirect main url : redirect:/main **/
    public static final String REDIRECT_MAIN = "redirect:/main";
    
    /** Redirect main url : redirect:/notice **/
    public static final String REDIRECT_NOTICE = "redirect:/notice";

    /** Redirect main url : redirect:/qna **/
    public static final String REDIRECT_QNA = "redirect:/qna";
    
    /** Return Attribute : return message **/
    public static final String RTN_MSG = "rtnMsg";
    
    /** Custom Exception Messages : errMsgs **/
    public static final String ERR_MSGS = "errMsgs";
    
    /** Return Attribute : gnb **/
    public static final String GNB = "GNB";
    
    /** Return Attribute : lnb **/
    public static final String LNB = "LNB";
    
    /** LNB Attribute Value : main **/
    public static final String LNB_MAIN = "main";
    
    /** LNB Attribute Value : clustermgmt **/
    public static final String LNB_CLUSTERMGMT = "clustermgmt";
   
    /** LNB Attribute Value : notice **/
    public static final String LNB_NOTICE = "notice";

    /** LNB Attribute Value : qna **/
    public static final String LNB_QNA = "qna";
    
    /** Login url : /login **/
    public static final String LOGIN = "/login";
    
    /** Main url : /main **/
    public static final String MAIN = "/main";

    /** Cluster Management Menu Entry url : /clustermgmt/cluster **/
    public static final String CLUSTERMGMT_ENTRY = "/clustermgmt/cluster";
    
    /** Namespace Management Entry url : /namespacemgmt/namespace **/
    public static final String NAMESPACEMGMT_ENTRY = "/namespacemgmt/namespace";
    
    /** Namespace Sub Menu Entry url : /autoenvmgmt/auto **/
    public static final String AUTOENVMGMT_SUB_ENTRY = "/autoenvmgmt/auto";
    
    /** Namespace Sub Menu Entry url : /istiomgmt/canaryMgmt **/
    public static final String ISTIOMGMT_SUB_ENTRY = "/istiomgmt/canaryMgmt";
    
        
    /** Arsenal Login Session Attribute name: loginSession **/
    public static final String LOGIN_SESSION = "loginSession";
    
    /** Return Attribute : USER has CA role For Cluster Master  **/
    public static final String IS_CLUSTER_ADMIN = "isClusterAdmin";
    
    /** Return Attribute : USER has CA && NA role For Namespace **/
    public static final String IS_ADMIN = "isAdmin";

    /** replace null to date : 9999/12/31 00:00:00 **/
    public static final String NULL_TO_DATE = "9999/12/31 00:00:00";
    
    /** Excel Download template folder : /static/excel/ **/
    public static final String EXCEL_TEMPLATE = "/static/excel/";
    
    /** Exception Logger Prefix : SERVER_EXCEPTION */
    public static final String SERVER_EXCEPTION = "SERVER_EXCEPTION";
    
    /** Exception Logger Prefix : SERVER_WARNING */
    public static final String SERVER_WARNING = "SERVER_WARNING";
    
    /** Exception Logger Prefix : SQL_EXCEPTION */
    public static final String SQL_EXCEPTION = "SQL_EXCEPTION";

    /** Exception Logger Prefix : CUSTOM_EXCEPTION */
    public static final String CUSTOM_EXCEPTION = "CUSTOM_EXCEPTION";
    
    /** Exception Logger Prefix : CUSTOM_ARSENAL_EXCEPTION */
    public static final String CUSTOM_ARSENAL_EXCEPTION = "CUSTOM_ARSENAL_EXCEPTION";
    
    /** Exception Logger Prefix : CUSTOM_LOGIN_EXCEPTION */
    public static final String CUSTOM_LOGIN_EXCEPTION = "CUSTOM_LOGIN_EXCEPTION";
    
    /** Exception Logger Prefix : CONSTRAINT_VIOLATION_EXCEPTION */
    public static final String CONSTRAINT_VIOLATION_EXCEPTION = "CONSTRAINT_VIOLATION_EXCEPTION";
    

    
    /** CLUSTER DOMAIN DOT : . **/
    public static final String DOT = ".";
    
    /** CLUSTER DOMAIN DASH : - **/
    public static final String DASH = "-";
    
    /** Resource Type : CPU **/
    public static final String CPU = "cpu";
    
    /** Resource Type : MEMORY **/
    public static final String MEMORY = "memory";
    
    /** Monitoring - AIP SUCCESS_LOG : SUCCESS  **/
    public static final String SUCCESS_LOG = "SUCCESS";
    
    /** Monitoring - AIP SUCCESS_LOG : EXCEPTION  **/
    public static final String EXCEPTION_LOG = "FAIL";
    
    /** Monitoring search start day : 5 day before from now  **/
    public static final Integer SEARCH_START_DAY = 5;
    
    /** Cluster User Privilege : CA[Cluster_Admin] **/
    public static final String CLUSTER_ADMIN = "CA";

    
    /** Namespace User - default : default **/
    public static final String DEFAULT = "default";
    

    /** Namespace Annotation Description : DESCRIPTION **/
    public static final String DESCRIPTION = "openshift.io/description";
	
	/** Namespace Annotation Displayname : DISPLAYNAME **/
	public static final String DISPLAYNAME = "openshift.io/display-name";
	
    /**
     * API body return type
     */
    public interface BODY_RETURN_TYPE {
        int MAP=1, JSON=2;
    }
    
    private ConstUtil() {
        throw new IllegalStateException("Non-instantiable Constant Class");
    }

    /**
     * xml object 를 String으로 변환
     * @param Node document
     * @return string
     */
    public static String getXmlString(Node doc) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        if ( !(doc instanceof Document) ) {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        }

        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);

        return result.getWriter().toString();

    }

    /**
     * object class 를 String으로 변환
     * @param Object
     * @return string
     * @throws JsonProcessingException
     */
    public static String getObjectPrettyString(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(object);
    }
}