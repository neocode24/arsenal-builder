package com.kt.arsenal.biz.builder.domain;

import com.kt.arsenal.biz.autoenvmgmt.service.JenkinsViewService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Arsenal-Dev Job 단위 구성 정보
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/09/04
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobInfo {

    /**
     * Job 명칭
     */
    private String name;

    /**
     * Job Url
     */
    private String url;

    /**
     * Job이 소속된 viewName
     */
    private String viewName;

    /**
     * Job에 대한 Main Git URL 정보. 다수 gitlab 정보가 기술된 경우, Parameter에서 사용됨
     */
    private String gitUrl;

    /**
     * Job에 대한 Main Git Group 정보.
     */
    private String gitGroupName;

    /**
     * Job에 대한 Main Git Project 명칭
     */
    private String gitProjectName;

    /**
     * Job에 대한 Main Git DevToolsId
     */
    private Long gitDevToolsId;

    /**
     * Job에 대한 필요한 Parameter
     * parameterMap (Key - parameter Name, Value - parameter 기본값)
     */
    private Map<String, Object> parameterDefinitionMap;

    /**
     * Job에 대한 평균 성공율 (Jenkins 최근 수행 갯수 기반)
     */
    private Integer avgStability;

    /**
     * Job의 최근 Build 이력
     */
    private BuildHistory buildHistory;
}
