package com.kt.arsenal.biz.portal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.arsenal.biz.builder.domain.BuilderRequestDomain;
import com.kt.arsenal.biz.builder.domain.GitlabResource;
import com.kt.arsenal.biz.builder.domain.JenkinsResource;
import com.kt.arsenal.biz.login.domain.SecurityUser;
import com.kt.arsenal.biz.portal.domain.BusinessTopologyDomain;
import com.kt.arsenal.common.exception.CustomArsenalException;
import com.kt.arsenal.common.exception.CustomStatusCd;
import com.kt.arsenal.common.model.RestMessage;
import com.kt.arsenal.common.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Arsenal-Dev
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/06/15
 */

@Slf4j
@Service
public class PortalCallerService {

    @Value("${spring.back-end.portal.http-uri:}")
    private String portalApiUri;

    @Value("${spring.back-end.portal.token:}")
    private String portalApiToken;


    /**
     * Rest Template
     */
    RestTemplate restTemplate;

    @Autowired
    public PortalCallerService(RestTemplateBuilder restTemplateBuilder) {
        restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
    }


    /**
     * Portal 에서 Jenkins DevTools 정보만 일괄 획득함.
     * @param builderRequestDomain
     */
    public void setDevToolsInfo(BuilderRequestDomain builderRequestDomain, List<BusinessTopologyDomain> businessTopologyDomains) {

        // Business Topology 에서 요청한 사업에 해당하는 Jenkins DevToolsId만 획득
        List<Long> jenkinsDevToolsIds =
            businessTopologyDomains.stream()
                    .filter(f -> f.getServiceAliasName().equals(builderRequestDomain.getServiceAliasName()))
                    .flatMap(topologyDomain -> topologyDomain.getClusters().stream().flatMap(cluster -> cluster.getDevTools().stream().filter(f -> "JENKINS".equals(f.getTypeCd()))))
                    .map(BusinessTopologyDomain.DevTools::getDevtoolId)
                    .collect(Collectors.toList());

        // Business Topology 에서 요청한 사업에 해당하는 Gitlab DevToolsId만 획득
        List<Long> gitlabDevToolsIds =
                businessTopologyDomains.stream()
                        .filter(f -> f.getServiceAliasName().equals(builderRequestDomain.getServiceAliasName()))
                        .flatMap(topologyDomain -> topologyDomain.getClusters().stream().flatMap(cluster -> cluster.getDevTools().stream().filter(f -> "GITLAB".equals(f.getTypeCd()))))
                        .map(BusinessTopologyDomain.DevTools::getDevtoolId)
                        .collect(Collectors.toList());


        // Arsenal Portal 에서 활성화 된 Jenkins DevToolsId 정보(Token, Url, Account 등) 정보 조회
        List<Map<String, Object>> response = callService("/arsenal/arsenalmgmt/findDevtools", createHeader(), HttpMethod.POST, null);


        // 사업에 유효한 Jenkins 정보만 취득.
        builderRequestDomain.setJenkinsResources(
            response.stream()
                    .filter(f -> "JENKINS".equals(f.get("typeCd")))
                    .filter(f -> "Y".equals(f.get("useYn")))
                    .filter(f -> jenkinsDevToolsIds.contains(((Number) f.get("devtoolId")).longValue()) )          // 사업에 일치하는 devtool 정보만 filter
                    .map(map ->
                            JenkinsResource.builder()
                                .devToolsId( ((Number) map.get("devtoolId")).longValue() )
                                .name((String) map.get("toolName"))
                                .accessToken((String) map.get("accessToken"))
                                .url((String) map.get("url"))
                                .account((String) map.get("account"))
                                .build())
                    .collect(toList())
        );

        // 사업에 유효한 Gitlab 정보만 취득.
        builderRequestDomain.setGitlabResources(
                response.stream()
                        .filter(f -> "GITLAB".equals(f.get("typeCd")))
                        .filter(f -> "Y".equals(f.get("useYn")))
                        .filter(f -> gitlabDevToolsIds.contains(((Number) f.get("devtoolId")).longValue()))
                        .map(map ->
                                GitlabResource.builder()
                                        .devToolsId( ((Number) map.get("devtoolId")).longValue() )
                                        .name((String) map.get("toolName"))
                                        .accessToken((String) map.get("accessToken"))
                                        .url((String) map.get("url"))
                                        .account((String) map.get("account"))
                                        .build())
                        .collect(toList())

        );

    }


    /**
     * Portal 사업리스트 계층 구조 조회
     * 기존 데이터 구조는 business, devtools, namespace, cluster 4개 항목이 수평구조이나, 아래와 같은 계층구조로 재구성함.
     *
     *   BusinessTopologyDomain
     *     +-- DevTools
     *     +-- Namespace
     *     +-- Cluster
     *
     *  계층 구조 과정에서 DevTools가 BusinessTopology 하위가 될때, BusinessServiceId로 확인하는데, 이때 Cluster 정보는 중복적으로 존재가능함.
     *  (Gitlab 같은 경우 하나의 Gitlab이 여러 Cluster에서 사용가능하기 때문)
     *  데이터 생성 후, 이와 같은 중복을 제거하고, Id에 맞게 순차 정렬함.
     *
     * @return
     * @throws JsonProcessingException
     */
    @Cacheable("INIT_BUSINESS_TOPOLOGY_DOMAIN")
    public List<BusinessTopologyDomain> initBusinessTopologyDomain() {

        // Portal 에서 전체 Business, Devtools, Namespace, Cluster 정보 조회
        Map<String, Object> responseMap = callService("/arsenal/arsenalmgmt/findBusinessClustersByUser", createHeader(), HttpMethod.POST, null);


        // Portal 정보 조회 후, 계층 구조로 만들기 위한 객체
        List<BusinessTopologyDomain> businessTopologyDomains = new ArrayList<>();


        // Business 응답결과 Travers - 상위 객체 생성
        List<Map<String, Object>> businessResponseMap = (List<Map<String, Object>>) responseMap.get("businesses");
        businessResponseMap.stream().forEach(map -> {

            // 최상위 Business 객체 생성
            businessTopologyDomains.add(
                BusinessTopologyDomain.builder()
                        .businessServiceId( ((Integer)map.get("businessServiceId")).longValue())
                        .serviceCd((String) map.get("serviceCd"))
                        .serviceName((String) map.get("serviceName"))
                        .unitServiceCd((String) map.get("unitServiceCd"))
                        .unitServiceName((String) map.get("unitServiceName"))
                        .serviceAliasName((String) map.get("serviceAliasName"))
                        .dashboardYn(map.get("dashboardYn").equals("Y") ? true : false)
                        .akcYn(map.get("akcYn").equals("Y") ? true : false)
                        .clusters(new ArrayList<>())
                        .build()
            );

        });




        // Cluster 응답결과 Travers - 하위 객체 생성
        List<Map<String, Object>> clusterResponseMap = (List<Map<String, Object>>) responseMap.get("clusters");
        clusterResponseMap.stream().forEach(map -> {

            try {
                // Clusters 순회중에 최상위 Business Domain을 가르키는 경우 해당 정보 하위로 생성.
                // (parent)business - (child)clusters 구조 생성.
                businessTopologyDomains
                        .stream()
                        .filter(f -> f.getBusinessServiceId().equals(((Integer)map.get("businessServiceId")).longValue()))
                        .findFirst().orElseThrow(NoSuchElementException::new).getClusters().add(

                        // 하위 Cluster 객체 생성
                        BusinessTopologyDomain.Cluster.builder()
                                .clusterMasterId(((Integer) map.get("clusterMasterId")).longValue())
                                .clusterName((String) map.get("clusterName"))
                                .clusterPlatform((String) map.get("clusterPlatform"))
                                .platformVersion((String) map.get("platformVersion"))
                                .environmentCd((String) map.get("environmentCd"))
                                .environmentName((String) map.get("environmentName"))
                                .useYn(map.get("useYn").equals("Y") ? true : false)
                                .supportYn(map.get("supportYn").equals("Y") ? true : false)
                                .collectorId( map.get("collectorId") != null ? ((Integer) map.get("collectorId")).longValue() : null)
                                .namespaces(new ArrayList<>())
                                .devTools(new ArrayList<>())
                                .build()
                );
            } catch (NoSuchElementException e) {
                log.debug("Clusters 정보에서 명시된 businessServiceId 정보가 존재하지 않습니다. 해당 Clusters 정보는 Topology 구성에서 무시합니다.[ClusterId:{}, Name:{]], [BusinessServiceId:{}]",
                        map.get("clusterMasterId"),
                        map.get("clusterName"),
                        map.get("businessServiceId")
                );
            }
        });



        // Namespace 응답결과 Travers - 하위 객체 생성
        List<Map<String, Object>> namespaceResponseMap = (List<Map<String, Object>>) responseMap.get("namespaces");
        namespaceResponseMap.stream().forEach(map -> {

            try {
                // Namespace 순회중에 최상위 Business Domain을 가르키는 경우 해당 정보 하위로 생성.
                // (parent)business - (child)cluster 구조 생성.
                businessTopologyDomains
                        .stream()
                        .filter(f -> f.getBusinessServiceId().equals(((Integer)map.get("businessServiceId")).longValue()))
                        .findFirst().orElseThrow(NoSuchElementException::new).getClusters()
                                .stream()
                                .filter(f -> f.getClusterMasterId().equals(((Integer)map.get("clusterMasterId")).longValue()))
                                .findFirst().orElseThrow(NoSuchElementException::new).getNamespaces().add(

                                        // 하위 Namespace 객체 생성
                                        BusinessTopologyDomain.Namespace.builder()
                                                .clusterMasterId(((Integer) map.get("clusterMasterId")).longValue())
                                                .namespaceId(((Integer) map.get("namespaceId")).longValue())
                                                .namespace((String) map.get("namespace"))
                                                .namespaceDisp((String) map.get("namespaceDisp"))
                                                .description((String) map.get("description"))
                                                .build()
                );
            } catch (NoSuchElementException e) {
                log.debug("Namespaces 정보에서 명시된 businessServiceId 또는 ClusterMasterId 정보가 존재하지 않습니다. 해당 Namespaces 정보는 Topology 구성에서 무시합니다.[NamespaceId:{}, Name:{}], [BusinessServiceId:{}] [ClusterMasterId:{}]",
                        map.get("namespaceId"),
                        map.get("namespaceDisp"),
                        map.get("namespaceId"),
                        map.get("clusterMasterId")
                );
            }
        });



        // DevTools 응답결과 Travers - 하위 객체 생성
        List<Map<String, Object>> devtoolsResponseMap = (List<Map<String, Object>>) responseMap.get("devtools");
        devtoolsResponseMap.stream().forEach(map -> {

            try {
                // DevTools 순회중에 최상위 Business Domain을 가르키는 경우 해당 정보 하위로 생성.
                // (parent)business - (child)devtools 구조 생성.
                businessTopologyDomains
                        .stream()
                        .filter(f -> f.getBusinessServiceId().equals(((Integer)map.get("businessServiceId")).longValue()))
                        .findFirst().orElseThrow(NoSuchElementException::new).getClusters()
                        .stream()
                        .filter(f -> f.getClusterMasterId().equals(((Integer)map.get("clusterMasterId")).longValue()))
                        .findFirst().orElseThrow(NoSuchElementException::new).getDevTools().add(

                        // 하위 DevTools 객체 생성
                        BusinessTopologyDomain.DevTools.builder()
                                .devtoolId(((Integer) map.get("devtoolId")).longValue())
                                .typeCd((String) map.get("typeCd"))
                                .toolName((String) map.get("toolName"))
                                .collectorId(((Integer) map.get("collectorId")).longValue())
                                .build()
                );
            } catch (NoSuchElementException e) {
                log.debug("DevTool 정보에서 명시된 businessServiceId 정보가 존재하지 않습니다. 해당 DevTools 정보는 Topology 구성에서 무시합니다. [DevToolsId:{}, Name:{}] [BusinessServiceId:{}] [ClusterMasterId:{}]",
                        map.get("devtoolId"),
                        map.get("toolName"),
                        map.get("businessServiceId"),
                        map.get("clusterMasterId")
                );
            }
        });


        // Cluster 부분. 계층 구조 생성과정에서 발생될 수 있는 중복 데이터 제거(Gitlab - devTools의 경우 DEV, PRD 둘다 존재하기에 중복 제거함. 이외 Jenkins - devTools, Namespace 중복은 오류 발생차원에서 정리)
        businessTopologyDomains.stream().forEach(business -> {
            business.setClusters(
                    business.getClusters()
                            .stream()
                            .filter(distinctByKeys(BusinessTopologyDomain.Cluster::getClusterMasterId))
                            .sorted(Comparator.comparing(BusinessTopologyDomain.Cluster::getClusterMasterId))
                            .collect(Collectors.toList())
            );
        });

        // Cluster 이하 부분. 계층 구조 생성과정에서 발생될 수 있는 중복 데이터 제거(Gitlab - devTools의 경우 DEV, PRD 둘다 존재하기에 중복 제거함. 이외 Jenkins - devTools, Namespace 중복은 오류 발생차원에서 정리)
        businessTopologyDomains.stream().forEach(business -> {
            // 사업 단위 Loop
            business.getClusters()
                    .stream().forEach(cluster -> {
                        // DevTools 중복 정리
                        cluster.setDevTools(
                                cluster.getDevTools()
                                        .stream()
                                        .filter(distinctByKeys(BusinessTopologyDomain.DevTools::getDevtoolId))
                                        .sorted(Comparator.comparing(BusinessTopologyDomain.DevTools::getDevtoolId))
                                        .collect(toList())
                        );
                        // Namespace 중복 정리
                        cluster.setNamespaces(
                                cluster.getNamespaces()
                                        .stream()
                                        .filter(distinctByKeys(BusinessTopologyDomain.Namespace::getClusterMasterId, BusinessTopologyDomain.Namespace::getNamespaceId))
                                            .sorted(Comparator.comparing(BusinessTopologyDomain.Namespace::getClusterMasterId)
                                                    .thenComparing(Comparator.comparing(BusinessTopologyDomain.Namespace::getNamespaceId))
                                            )
                                        .collect(toList())
                        );
            });
        });

        return businessTopologyDomains;
    }


    /**
     * Portal service 호출하여, Token이 정상(returnCode:OK)이면 true, 그 외에는 false
     * @param requestHttpHeaders
     * @return
     */
    @Cacheable(cacheNames = "VALIDATE_REQUEST_TOKEN")
    public UserDetails isValidateRequestToken(HttpHeaders requestHttpHeaders) {
        Map<String, Object> responseMap = callService("/arsenal/getUserInfo", requestHttpHeaders, HttpMethod.POST, null);

        SecurityUser userDetails = null;
        try {
            userDetails = new SecurityUser(
                    (String) responseMap.get("username"),
                    (String) responseMap.get("password"),
                    new HashMap<String, String>() {
                        {
                            put("userName", (String) responseMap.get("name"));
                            put("deptName", (String) responseMap.get("deptNm"));
                            put("companyName", (String) responseMap.get("companyNm"));
                            put("mobile", (String) responseMap.get("telNo"));
                            put("email", (String) responseMap.get("email"));
                            put("accessIp", (String) responseMap.get("accessIp"));
                            put("macAddr", (String) responseMap.get("macAddr"));
                            put("statusCd", (String) responseMap.get("statusCd"));
                        }
                    },
                    ((List<Map<String, String>>) responseMap.get("authorities"))
                        .stream()
                        .flatMap(authority -> authority.values().stream())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList())

            );


        } catch (Exception e) {
            log.error("Token 정보에서 사용자 정보를 취득하는 과정에서 오류가 발생되었습니다.", e);
            throw new CustomArsenalException(CustomStatusCd.CD_31001.getCd(), CustomStatusCd.CD_31001.getMsg());
        }

        return userDetails;
    }







    /**
     * Prometheus를 호출하기 위한 Header.
     * @return HttpHeaders
     */
    private HttpHeaders createHeader() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + portalApiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        return headers;
    }



    /**
     * Back-End Portal 서비스 호출(GET/POST 지원)
     * @param uri Back-End Portal URI
     * @param httpMethod GET/POST
     * @param parameterMap HttpEntity with Parameter
     * @return
     */
    private <T> T callService(String uri, HttpHeaders header, HttpMethod httpMethod, MultiValueMap<String, String> parameterMap) {
        // 응답 객체
        RestMessage responseMessage = null;

        try {
            ResponseEntity<?> responseEntity = restTemplate.exchange(
                    new URI(portalApiUri + uri),
                    httpMethod,
                    parameterMap != null ? new HttpEntity<>(parameterMap, header) : new HttpEntity<>(header),
                    Map.class
            );

            responseMessage = new ObjectMapper().convertValue(responseEntity.getBody(), new TypeReference<RestMessage>(){});
            if ( !responseMessage.getReturnCode().equals("OK") ) {
                log.error("ErrorMessage : {}", responseMessage.getMessage());
                throw new CustomArsenalException(CustomStatusCd.CD_31001.getCd(), responseMessage.getMessage());
            }

        } catch (URISyntaxException e) {
            log.error("URL Encoding 중에 오류가 발생하였습니다.", e);
            throw new CustomArsenalException(CustomStatusCd.CD_31001.getCd(), CustomStatusCd.CD_31001.getMsg());
        } catch (RestClientException e) {
            log.error("Portal 서비스 데이터 조회중 오류가 발생하였습니다. ", e);
            throw new CustomArsenalException(CustomStatusCd.CD_31001.getCd(), CustomStatusCd.CD_31001.getMsg());
        }

        return (T) responseMessage.getData();
    }


    /**
     * Stream에서 복수 Key로 중복 제거 하기 위한 함수.
     * @param keyExtractors
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    private static <T> Predicate<T> distinctByKeys(Function<? super T, ?>... keyExtractors) {
        final Map<List<?>, Boolean> seen = new ConcurrentHashMap<>();

        return t -> {
            final List<?> keys = Arrays.stream(keyExtractors)
                    .map(ke -> ke.apply(t))
                    .collect(toList());

            return seen.putIfAbsent(keys, Boolean.TRUE) == null;
        };
    }
}
