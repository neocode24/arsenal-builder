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


/**
 * Ajax 조회 결과 처리 인터페이스
 * @author 91218672
 * @since 2018.12.11.
 * @version 1.0.0
 * @see
 */
public interface GenericMessageMutator {

   boolean isCustomeMessage();

   Object getData();

   String getMessage();

   String getReturnCode();

   void transformMessage(LocaleAwareMessageService service);
}
