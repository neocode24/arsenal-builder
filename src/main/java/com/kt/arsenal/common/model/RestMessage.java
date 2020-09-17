/*
 * Arsenal-Platform version 1.0
 * Copyright ⓒ 2019 kt corp. All rights reserved.
 * This is a proprietary software of kt corp, and you may not use this file except in
 * compliance with license agreement with kt corp. Any redistribution or use of this
 * software, with or without modification shall be strictly prohibited without prior written
 * approval of kt corp, and the copyright notice above does not evidence any actual or
 * intended publication of such software.
 */
package com.kt.arsenal.common.model;

import java.io.Serializable;


/**
 * Ajax 조회 결과 설정 객체
 * @author 
 * @since 
 * @version 1.0.0
 * @see
 */
public class RestMessage implements GenericMessage, GenericMessageMutator, Serializable {

    private static final long serialVersionUID = -7596793070632407479L;
    
   /**
    * 정상
    */
    public static final String OK = "OK";

   /**
    * 실패
    */
    public static final String NG = "NG";
    
    /**
     * Login Exception
     */
    public static final String LE = "LE";

    private String returnCode = "";

    private String message = "";

    private Object data = null;

    private boolean customeMessage = true;

    @Override
    public void setOK() {
        this.setReturnCode(OK);
    }

    @Override
    public void setNG() {
        this.setReturnCode(NG);
    }
    
    @Override
    public void setLE() {
        this.setReturnCode(LE);
    }

    @Override
    public void enableCustomeMessage() {
        customeMessage = true;
    }

    @Override
    public void transformMessage(LocaleAwareMessageService service) {
        if (this.message != null && !this.message.equals("")) {
            this.message = service.getMessage(this.message);
        }
    }

    @Override
    public String getReturnCode() {
        return returnCode;
    }

    @Override
    public void setReturnCode(String returnCode) {
        this.returnCode = returnCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public boolean isCustomeMessage() {
        return customeMessage;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("RestMessage{").append("returnCode='").append(returnCode).append('\'').append(", message='")
                .append(message).append('\'');

        if (data != null) {
            sb.append(", data=").append(data.toString());
        }
        sb.append(", customeMessage=").append(customeMessage).append('}');

        return sb.toString();
    }
 
}
