package com.kt.arsenal.biz.builder.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Arsenal-Dev GitProject Info
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/09/04
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitProjectInfo {


    /**
     * Git Project URL
     */
    private String url;

    /**
     * Git Project Group 명칭
     */
    private String groupName;

    /**
     * Git Project 명칭
     */
    private String projectName;

    /**
     * Git Project DevToolsId
     */
    private Long devToolsId;
}
