package com.kt.arsenal.biz.builder.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Arsenal-Dev
 *
 * BuilderRequestDomain을 상속하여,
 * 필요한 serviceAlias 명칭과 Jenkins, Gitlab에 대한 접근 정보 Field를 그대로 사용한다.
 *
 * Builder 상속 관계를 위해 SuperBuilder를 사용하였고, 이에 따라 Lombok 버전을 1.18.12 버전으로 사용함.
 *
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/09/07
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuilderJobRequestDomain extends BuilderRequestDomain {

    /**
     * Job 정보. Jenkins 정보에서 획득한 Job 정보를 그대로 입력 받음.
     * 조회한 Job의 ParameterMap 정보는 그대로 반환하지만, Parameter에 List가 필요한 경우 List로 조회하여 제공함.
     * 그 외 값이 조회되지 않으면, 기본 값을 그대로 제공함.
     */
    private JobInfo jobInfo;
}
