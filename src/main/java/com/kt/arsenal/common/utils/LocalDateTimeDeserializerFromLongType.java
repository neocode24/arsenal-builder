/*
 * Arsenal-Platform version 1.0
 * Copyright ⓒ 2019 kt corp. All rights reserved.
 * This is a proprietary software of kt corp, and you may not use this file except in
 * compliance with license agreement with kt corp. Any redistribution or use of this
 * software, with or without modification shall be strictly prohibited without prior written
 * approval of kt corp, and the copyright notice above does not evidence any actual or
 * intended publication of such software.
 */
package com.kt.arsenal.common.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

/**
 * Arsenal-Dev LocalDateTimeDeserializer From Long Type
 *
 * @author 82022961
 * @version 1.0.0
 * @see
 * @since 30/09/2019
 */
public class LocalDateTimeDeserializerFromLongType extends StdDeserializer<LocalDateTime> {

    protected LocalDateTimeDeserializerFromLongType() {
        super(LocalDateTime.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.valueOf(parser.readValueAs(String.class))), TimeZone.getDefault().toZoneId());
    }
}
