package com.kt.arsenal.biz.builder.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kt.arsenal.common.utils.LocalDateTimeDeserializer;
import com.kt.arsenal.common.utils.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Arsenal-Dev Jenkins LastBuild 정보
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/09/10
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastBuildInfo {

    /**
     * Name
     */
    private String name;

    /**
     * URL
     */
    private String url;

    /**
     * 가장 최근 빌드 넘버
     */
    private Integer number;

    /**
     * 빌드 진행 상태
     */
    private boolean building;

    /**
     * 가장 최근 빌드 상태
     */
    private String result;

    /**
     * build timestamp
     */
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime timestamp;

    /**
     * 가장 최근 빌드 Parameter
     * Parameter - "_class": "hudson.model.ParametersAction"
     */
    private Map<String, Object> buildParameterMap;

    /**
     * 가장 최근 빌드 수행자 ID
     */
    private String causeUserId;

    /**
     * 가장 최근 빌드 수행자 명칭
     */
    private String causeUserName;

    /**
     * 가장 최근 빌드 소요시간
     */
    private Integer duration;

    /**
     * 평균 빌드 소요시간
     */
    private Integer estimatedDuration;
}
