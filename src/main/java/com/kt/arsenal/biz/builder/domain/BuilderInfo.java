package com.kt.arsenal.biz.builder.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Arsenal-Dev Jenkins View Info
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/09/04
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuilderInfo {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JenkinsResource {

        /**
         * Name
         */
        private String name;

        /**
         * 관리되는 DevToolsId
         */
        private Long devToolsId;

        /**
         * Url
         */
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitlabResource {


        /**
         * Name
         */
        private String name;

        /**
         * 관리되는 DevToolsId
         */
        private Long devToolsId;

        /**
         * Url
         */
        private String url;

    }


    /**
     * Jenkins Resource 정보
     */
    private JenkinsResource jenkins;

    /**
     * Gitlab Resource 정보
     * Jenkins에서 다수의 Gitlab를 사용하는 경우 List 일 수 있음.
     */
    private List<GitlabResource> gitlabResources;

    /**
     * jenkins View 정보
     */
    private List<ViewInfo> views;
}
