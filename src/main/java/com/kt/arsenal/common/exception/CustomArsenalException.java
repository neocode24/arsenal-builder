/*
 * Arsenal-Platform version 1.0
 * Copyright ⓒ 2019 kt corp. All rights reserved.
 * This is a proprietary software of kt corp, and you may not use this file except in
 * compliance with license agreement with kt corp. Any redistribution or use of this
 * software, with or without modification shall be strictly prohibited without prior written
 * approval of kt corp, and the copyright notice above does not evidence any actual or
 * intended publication of such software.
 */
package com.kt.arsenal.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * CustomArsenalException Exception 정보 객체
 * @author 
 * @since 
 * @version 1.0.0
 * @see
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CustomArsenalException extends RuntimeException {

    private static final long serialVersionUID = -6531321298357306786L;
    
    /** (사용자 정의) Http 상태 코드 값 **/
    //private final Integer status;
    private Integer status;
    
    /** (사용자 정의) Http 상태 메시지(에러 메시지) **/
    //private final String message;
    private String message;
    
    /** 실제 발생한 exception 객체 **/
    private Throwable cause;
    
    public CustomArsenalException(String message) {
        super(message);
        this.message = message;
    }

    public CustomArsenalException(String message, Throwable t) {
        super(message, t);
        this.message = message;
    }

    
    public CustomArsenalException(Integer status, String message) {
        super(message);
        this.status = status;
        this.message = message;
    }

    public CustomArsenalException(Integer status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.message = message;
        this.cause = cause;
    }

}
