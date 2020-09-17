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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;


/**
 * Ajax 조회 결과 메시지 로케일 처리
 * @author 91218672
 * @since 2018.12.11.
 * @version 1.0.0
 * @see
 */
@Component
public class LocaleAwareMessageService {

   @Autowired
   private MessageSource messageSource;

  public String getMessage(String code){
       return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
   }

}
