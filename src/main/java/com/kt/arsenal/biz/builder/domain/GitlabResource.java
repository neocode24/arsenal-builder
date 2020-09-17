package com.kt.arsenal.biz.builder.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Arsenal-Dev
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/09/04
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitlabResource {


    /**
     * Name
     */
    private String name;

    /**
     * 관리되는 DevToolsId
     */
    private Long devToolsId;

    /**
     * Token
     */
    private String accessToken;

    /**
     * Url
     */
    private String url;

    /**
     * Account
     */
    private String account;
}
