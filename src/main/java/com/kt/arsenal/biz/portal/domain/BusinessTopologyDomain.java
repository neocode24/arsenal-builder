package com.kt.arsenal.biz.portal.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Arsenal-Dev
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/06/15
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessTopologyDomain implements Serializable {

    /**
     * Business Service Id : 사업 정의 아이디
     */
    Long businessServiceId;
    /**
     * 서비스 코드
     */
    String serviceCd;
    /**
     * 서비스 명
     */
    String serviceName;
    /**
     * 단위 서비스 코드
     */
    String unitServiceCd;
    /**
     * 단위 서비스 명
     */
    String unitServiceName;
    /**
     * 서비스 별칭
     */
    String serviceAliasName;
    /**
     * Dashboard 사용여부
     */
    boolean dashboardYn;
    /**
     * AKC 센터 입소 여부
     */
    boolean akcYn;


    /**
     * 하위 정보 - Cluster
     */
    List<Cluster> clusters;




    /**
     * Cluster 계층 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cluster {
        /**
         * Cluster Master Id (PK)
         */
        Long clusterMasterId;
        /**
         * Cluster Name
         */
        String clusterName;
        /**
         * Cluster Platform 명칭
         */
        String clusterPlatform;
        /**
         * Cluster Platform Version
         */
        String platformVersion;
        /**
         * DEV, PRD등 환경
         */
        String environmentCd;
        /**
         * 환경 명칭
         */
        String environmentName;
        /**
         * 사용유무
         */
        boolean useYn;
        /**
         * 지원 Cluster 여부
         */
        boolean supportYn;
        /**
         * CollectorId
         */
        Long collectorId;

        /**
         * 하위 정보 - Namespace
         */
        List<Namespace> namespaces;

        /**
         * 하위 정보 - DevTools (Jenkins, Gitlab)
         */
        List<DevTools> devTools;
    }


    /**
     * DevTools 계층 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DevTools {
        /**
         * DevlTools Id (PK)
         */
        Long devtoolId;
        /**
         * Type Cd : JENKINS, GITLAB
         */
        String typeCd;
        /**
         * Tool Name : Jenkins 천안DEV, Gitlab 천안공통 등
         */
        String toolName;
        /**
         * Collector Id
         */
        Long collectorId;
    }

    /**
     * Namespace 계층 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Namespace {
        /**
         * NamespaceId (PK)
         */
        Long namespaceId;
        /**
         * Namespace 명칭
         */
        String namespace;
        /**
         * Namespace 표시 명칭
         */
        String namespaceDisp;
        /**
         * Namespace 설명
         */
        String description;
        /**
         * Cluster Master Id
         */
        Long clusterMasterId;

        Long deploymentsCount;

        Long servicesCount;
    }




}
