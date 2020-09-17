package com.kt.arsenal.biz.builder.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Arsenal-Dev View 단위 구성 정보
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/09/04
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewInfo {


    /**
     * View 명칭
     */
    private String name;

    /**
     * View URL
     */
    private String url;

    /**
     * View 에 포함된 Job List
     */
    private List<JobInfo> jobs;
}
