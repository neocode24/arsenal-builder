package com.kt.arsenal.biz.builder.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * Arsenal-Dev Builder Request Domain
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/09/02
 */

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BuilderRequestDomain implements Serializable {

    /**
     * 사업 별칭
     */
    private String serviceAliasName;

    /**
     * 사업에서 사용하는 Gitlab 접속 정보.
     * 보통 Gitlab은 1건이나, 다수 건이 존재 할 수 있음.
     */
    private List<GitlabResource> gitlabResources;

    /**
     * 사업에서 사용하는 Jenkins 접속 정보.
     * DEV, SIT, PRD 정보를 Portal로 부터 조회하여 생성함.
     */
    private List<JenkinsResource> jenkinsResources;


}
