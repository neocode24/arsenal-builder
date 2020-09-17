package com.kt.arsenal.biz.autoenvmgmt.service;

import com.kt.arsenal.biz.builder.domain.*;
import com.kt.arsenal.common.constant.ConstUtil;
import com.kt.arsenal.common.exception.CustomArsenalException;
import com.kt.arsenal.common.exception.CustomStatusCd;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Arsenal-Dev
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/06/03
 */

@Slf4j
@Service
public class JenkinsViewService {

    /**
     * Jenkins 에서 사용되는 Arsenal 사용자 ID 명칭의 Hidden Field
     */
    public static final String ARSENAL_EXECUTOR_FIELD_ID                = "arsenalExecutorId";

    /**
     * Jenkins 에서 사용되는 Arsenal 사용자 Name 명칭의 Hidden Field
     */
    public static final String ARSENAL_EXECUTOR_FIELD_NAME              = "arsenalExecutorName";



    @Autowired
    private JenkinsService jenkinsService;

    @Autowired
    private JenkinsItemService jenkinsItemService;





    /**
     * Jenkins View 정보 구축
     * @param requestDomain
     * @return
     */
    @Cacheable("BUILDER_TOPOLOGY")
    public List<BuilderInfo> createViewTopology(BuilderRequestDomain requestDomain) {

        List<BuilderInfo> builderInfos = new ArrayList<>();

        // Jenkins 마다 병렬 실행
        requestDomain.getJenkinsResources().parallelStream().forEach(resource -> {

            log.info("   +-- DevToolsId:[{}] Jenkins View 정보를 구성합니다.", resource.getDevToolsId());
            BuilderInfo builderInfo = createViews(resource, requestDomain.getServiceAliasName(), requestDomain.getGitlabResources());

            if ( builderInfo.getViews() != null && builderInfo.getViews().size() > 0 ) {
                builderInfos.add(builderInfo);
                log.info("   +-- DevToolsId:[{}] 에 대한 view 정보[{}]를 찾아서 기록하였습니다.", resource.getDevToolsId(), requestDomain.getServiceAliasName());
            }
        });

        // devToolsId 기준으로 정렬
        builderInfos.sort(Comparator.comparing(jenkinsViewInfo -> jenkinsViewInfo.getJenkins().getDevToolsId()));

        return builderInfos;
    }



    /**
     * Jenkins View 정보 구성
     * @param resource              JenkinsResource
     * @param serviceAliasName      사업 별칭 (Jenkins View 정보와 동일)
     * @param gitlabResources       GitlabResource 정보 일체
     */
    public BuilderInfo createViews(JenkinsResource resource, String serviceAliasName, List<GitlabResource> gitlabResources) {

        log.info("  -- Jenkins Url:[{}] View 정보 수집을 시작합니다. --", resource.getUrl());

        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(resource.getAccessToken(), resource.getAccount()));

        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) jenkinsService.callJenkinsServer(
                resource.getUrl() + "/api/json?tree=views[name,url]",
                HttpMethod.GET,
                requestEntity,
                Map.class
        );

        return
                BuilderInfo.builder()
                    .gitlabResources(                                                       // Gitlab Token 과 Account 는 보안상 제외 하고 Client 제공 함.
                            gitlabResources.stream()
                                    .map(g ->
                                            BuilderInfo.GitlabResource.builder()
                                                    .name(g.getName())
                                                    .devToolsId(g.getDevToolsId())
                                                    .url(g.getUrl())
                                            .build()
                                    )
                                    .collect(Collectors.toList())
                    )
                    .jenkins(                                                               // Jenkins Token 과 Account 는 보안상 제외 하고 Client 제공 함.
                            BuilderInfo.JenkinsResource.builder()
                                    .name(resource.getName())
                                    .devToolsId(resource.getDevToolsId())
                                    .url(resource.getUrl())
                            .build()

                    )
                    .views(
                        Optional.ofNullable( (List<Map<String, Object>>)responseEntity.getBody().get("views")).map(Collection::stream).orElse(Stream.empty())
                                .filter(f -> serviceAliasName.equals(f.get("name")))                            // 요청한 serviceAlias 명칭이 일치하는 view 정보만 취득
                                .map( map ->
                                        ViewInfo.builder()                                                      // view 정보에 대한 map 구성
                                            .name((String) map.get("name"))
                                            .url((String) map.get("url"))
                                            .jobs(getJobsWithView(resource, serviceAliasName, gitlabResources))
                                            .build()
                                )
                                .filter(f -> f.getJobs().size() > 0)                                            // Job 이 존재하는 경우만
                                .limit(1)                                                                       // 하나의 Jenkins에 복수의 view가 있을 수 없음. 그러나, 만약을 위해 복수 자료 구조(List)로 설정함.
                                .collect(Collectors.toList())
                    )
                    .build();
    }


    /**
     * View에 포함된 Job 정보 조회
     * @param resource              JenkinsResource
     * @param viewName      사업 별칭 (Jenkins View 정보와 동일)
     * @param gitlabResources       GitlabResource 정보 일체
     * @return
     */
    public List<JobInfo> getJobsWithView(JenkinsResource resource, String viewName, List<GitlabResource> gitlabResources) {

        log.debug("devToolsId:[{}], viewName:[{}]을 조회 합니다.", resource.getDevToolsId(), viewName);

        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(resource.getAccessToken(), resource.getAccount()));

        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) jenkinsService.callJenkinsServer(
                resource.getUrl() + "/view/" + viewName +"/api/json",
                HttpMethod.GET,
                requestEntity,
                Map.class
        );


        // 조회 결과용 Job 정보 리스트
        List<JobInfo> jobs = new ArrayList<>();


        // view에 해당하는 job(item)의 정보 획득
        Optional.ofNullable((List<Map<String, Object>>)responseEntity.getBody().get("jobs")).map(Collection::parallelStream).orElse(Stream.empty())
                .filter(f -> ((String) f.get("name")).endsWith("-DOCKERIZE") || ((String) f.get("name")).endsWith("-TAG") || ((String) f.get("name")).endsWith("-DEPLOY") )     // 추후 선택 필요
                .forEach(job -> {


                    log.debug("devToolsId:[{}], jobName:[{}]에 대한 조회를 시작합니다.", resource.getDevToolsId(), String.valueOf(job.get("name")));



                    // Job - 상세 정보 획득
                    Map<String, Object> jobDetailInfo = getJobDetailInfo(resource, viewName, String.valueOf(job.get("name")));


                    // Job에 대한 Config.xml Document 획득
                    Document document = getJenkinsConfigDocument(getJenkinsItemConfigXmlString(resource, (String) job.get("url")));


                    // Job에 명시된 gitlab 정보 획득
                    GitProjectInfo gitProjectInfo = getGitProjectInfo(document, gitlabResources);


                    // Config.xml Document에서 Arsenal Hidden Parameter 존재 여부
                    if ( !isExistArsenalHiddenParameterMap(document) ) {
                        log.info("-- Arsenal Hidden Parameter 가 확인되지 않아, 새롭게 추가 합니다. [devToolsId:{}], [jobName:{}]", resource.getDevToolsId(), job.get("name"));
                        String newConfigXml = addArsenalHiddenParameterMapConfig(document);

                        if ( jenkinsItemService.updateItem(resource.getAccessToken(), resource.getUrl(), resource.getAccount(), (String) job.get("name"), newConfigXml) == null ) {
                            log.error("Arsenal Hidden Parameter를 생성 처리하는 과정에서 오류가 발생되었습니다.");
                            throw new CustomArsenalException(CustomStatusCd.CD_500.getCd(), CustomStatusCd.CD_500.getMsg());
                        }

                        // Jenkins Hidden Parameter 추가 후 다시 읽음.
                        document = getJenkinsConfigDocument(getJenkinsItemConfigXmlString(resource, (String) job.get("url")));
                    }

                    /*
                     * Parameter 찾는 부분을 Jenkins Json API를 사용하였으나,
                     * Json API에서 Parameter 개수가 많아지면, 일부만 찾아지고, 나머지는 찾지 못하는 현상이 발생되어, XML 설정파일을 parsing 하여 처리 하도록 함.
                     * 아래 기능은 Json API 처리 부분이나, 불필요한 사항으로 남겨둠.
                     */
                    // Job 상세 정보에서 property 항목에서 parameterDefinitions 를 찾아서, name, value 만 parameterMap으로 만듬.
                    // Parameter 값을 찾을대, Default속성이 있다면 default 값을 찾고, 없으면 "" 으로 초기화 함.
                    // 결과는 Map으로 생성.
                    Map<String, Object> parameterMap = getParameterMap(document);


                    // Job 상세 정보에서 최근 수행한 빌드 이력의 Score를 획득.
                    Integer avgStability = (Integer) Optional.ofNullable((List<Map<String, Object>>)jobDetailInfo.get("healthReport"))
                            .map(Collection::stream)                        // Job 상세 정보에서 healthReport 결과는 List로 존재.
                            .orElse(Stream.empty())                         // 없는 경우를 대비한 Empty Stream
                            .filter(Objects::nonNull)
                            .findFirst()                                    // healthReport는 List로 존재하지만, 동일한 값이 다수 있기 때문에 아무거나 하나만 사용.
                            .map(m -> m.get("score"))                       // healthReport List 아무거나 중에 score 결과를 획득.
                            .orElse(0);                               // 이 모든게 해당 없으면 0으로 대체



                    // Job 상세 정보에서 최근 빌드 Number를 획득.
                    Integer lastBuildNumber = (Integer) Optional.ofNullable((Map<String, Object>)jobDetailInfo.get("lastBuild")).map(m -> m.get("number")).orElse(0);
                    // Job 상세 정보에서 최근 빌드 Uri를 획득.
                    String lastBuildUri = (String) Optional.ofNullable((Map<String, Object>)jobDetailInfo.get("lastBuild")).map(m -> m.get("url")).orElse("");



                    // Job - 최근 빌드 이력 획득
                    Map<String, Object> lastBuildHistory = getBuildHistory(resource.getAccessToken(), lastBuildUri, resource.getAccount());

                    // Job 실행에 사용한 실제 Running ParameterMap 획득
                    Map<String, Object> lastBuildParameterMap =
                            Optional.ofNullable( (List<Map<String, Object>>)lastBuildHistory.get("actions") ).map(Collection::stream).orElse(Stream.empty())                                // actions 결과는 List. 없는 경우 Empty Stream
                                    .filter(action -> action.get("parameters") != null)                                                                                                     // action 중에 "parameters" List 획득.

                                    .flatMap(action -> ((List<Map<String, Object>>)action.get("parameters")).stream().filter(f -> ((String)f.get("_class")).endsWith("ParameterValue")))    // "ParameterValue" 유형만 선별.
                                    .collect(Collectors.toMap(
                                            parameter -> (String) parameter.get("name"),                        // "ParameterValue 유형에서 name을 key로 인식
                                            parameter -> parameter.get("value"),                                // "ParameterValue 유형에서 value를 value로 인식
                                            (m1, m2) -> m1.equals(m2) ? m2 : m1                                 // Key 중복이 발생된 경우 value m1, m2 중 m1 을 사용한다. 동일한 parameter Key에 value는 첫번째를 취득하는 의미.
                                    ));

                    // Job 실행 사용자 정보 획득
                    Map<String, Object> lastCauseMap =
                            Optional.ofNullable( (List<Map<String, Object>>)lastBuildHistory.get("actions") ).map(Collection::stream).orElse((Stream.empty()))                              // actions 결과는 List. 없는 경우 Empty Stream
                                    .filter(cause -> cause.get("causes") != null)                                                                                                           // action 중에 "cause" List 획득.

                                    .flatMap(cause -> ((List<Map<String, Object>>)cause.get("causes")).stream().filter(f -> ((String)f.get("_class")).endsWith("UserIdCause")))             // "UserIdCause" 유형만 선별.
                                    .findFirst().orElseGet(HashMap::new);
                    lastCauseMap.remove("shortDescription");                                                // 불필요한 Key 제거
                    lastCauseMap.remove("_class");






                    // put in jobs
                    jobs.add(
                            JobInfo.builder()
                                    .name((String) job.get("name"))
                                    .url((String) job.get("url"))
                                    .viewName(viewName)
                                    .gitUrl(gitProjectInfo.getUrl())
                                    .gitGroupName(gitProjectInfo.getGroupName())
                                    .gitProjectName(gitProjectInfo.getProjectName())
                                    .gitDevToolsId(gitProjectInfo.getDevToolsId())
                                    .parameterDefinitionMap(parameterMap)
                                    .avgStability(avgStability)
                                    .buildHistory(
                                            BuildHistory.builder()
                                                    .lastBuildNumber(lastBuildNumber)
                                                    .lastBuildUri(lastBuildUri)
                                                    .lastBuildStatus((String) lastBuildHistory.get("result"))
                                                    .lastBuildDateTime(
                                                            Objects.isNull(lastBuildHistory.get("timestamp")) ? null :
                                                            LocalDateTime.ofInstant(Instant.ofEpochMilli((Long) lastBuildHistory.get("timestamp")), TimeZone.getDefault().toZoneId())
                                                    )
                                                    .lastBuildParameterMap(lastBuildParameterMap)
                                                    .causeUserId((String) lastCauseMap.get("userId"))
                                                    .causeUserName((String) lastCauseMap.get("userName"))
                                                    .duration((Integer) lastBuildHistory.get("duration"))
                                                    .estimatedDuration((Integer) lastBuildHistory.get("estimatedDuration"))
                                                    .build()
                                    )
                                    .build()
            );

            log.info("      +-- [Job:{}] --", job.get("name"));
            log.info("          -- [lastBuild info number:[{}], uri:[{}], parameter:[{}]", lastBuildNumber, lastBuildUri, lastBuildParameterMap.toString());
        });

        // 최근 실행 된 Job 순서로 정렬
        jobs.sort(Comparator.comparing(jobInfo -> jobInfo.getBuildHistory().getLastBuildDateTime(), Comparator.nullsLast(Comparator.reverseOrder())));

        return jobs;
    }

    /**
     * Jenkins Job에 대한 마지막 Build 정보 획득
     * @param resource
     * @param viewName
     * @param jobName
     * @return
     */
    public Map<String, Object> getJobDetailInfo(JenkinsResource resource, String viewName, String jobName) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(resource.getAccessToken(), resource.getAccount()));

        // PRD-DEPLOY의 경우 서브프로젝트가 큰 경우 성능 저하 발생으로, 필요한 요소만 Json REST APIF를 사용함.
        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) jenkinsService.callJenkinsServer(
                resource.getUrl() + "/view/" + viewName + "/job/" + jobName + "/api/json?tree=builds[number,url],healthReport[description,score],lastBuild[number,url]",
                HttpMethod.GET,
                requestEntity,
                Map.class
        );

        return Optional.ofNullable(responseEntity.getBody()).orElseGet(HashMap::new);
    }

    /**
     * Jenkins Build history 정보 획득
     * @param token
     * @param uri
     * @param id
     * @return
     */
    public Map<String, Object> getBuildHistory(String token, String uri, String id) {

        // 빌드 이력 URI 주소가 없는 경우 empty 응답 값 반환
        if ( StringUtils.isEmpty(uri) ) return new HashMap<>();

        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(token, id));

        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) jenkinsService.callJenkinsServer(
                uri + "/api/json",
                HttpMethod.GET,
                requestEntity,
                Map.class
        );

        return Optional.ofNullable(responseEntity.getBody()).orElseGet(HashMap::new);
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class NameValue {
        String name;
        Object value;
    }

    /**
     * Node에서 "name", "defaultValue" 2개 태그를 찾아, 객체로 제공
     * defaultValue는 없을 수도 잇음.
     *
     * @param node
     * @return
     */
    private static NameValue nameValue(Node node) {

        Object value = "";
        try {
            Node defaultValueNode = matchNode(node.getChildNodes(), "defaultValue");
            if ( node.getNodeName().endsWith("BooleanParameterDefinition") ) {
                value = Boolean.parseBoolean(defaultValueNode.getTextContent());
            } else {
                value = defaultValueNode.getTextContent();
            }
        } catch (NoSuchElementException e) {
            log.debug("defaultValue가 Tag에 존재하지 않습니다. 생략하고 기본값:null 로 예외 처리 합니다.");
        }

        return
            NameValue.builder()
                .name(matchNode(node.getChildNodes(), "name").getTextContent())
                .value(value)
                .build();
    }

    /**
     * XML Node에서 특정 문자열로 일치하는 Node를 찾는 함수
     * @param items
     * @param matchNodeName
     * @return
     */
    private static Node matchNode(NodeList items, String matchNodeName) {
        return
                IntStream.range(0, items.getLength())
                    .mapToObj(items::item)
                    .filter(f -> f.getNodeName().equals(matchNodeName))
                    .findFirst()
                    .orElseThrow(NoSuchElementException::new);
    }

    /**
     * XML Node에서 특정 문자열로 끝나는 Node를 찾는 함수
     * @param items
     * @param endWithNodeName
     * @return
     */
    private static List<Node> filterNode(NodeList items, String endWithNodeName) {
        return
                IntStream.range(0, items.getLength())
                    .mapToObj(items::item)
                    .filter(f -> f.getNodeName().endsWith(endWithNodeName))
                    .collect(Collectors.toList());
    }



    private boolean isExistArsenalHiddenParameterMap(Document document) {

        // Hidden Parameter 자체가 없는 경우 Arsenal Hidden Parameter가 없는 것으로 처리 함.
        NodeList hiddenParameterDefinitions = document.getElementsByTagName("com.wangyin.parameter.WHideParameterDefinition");
        if ( hiddenParameterDefinitions.getLength() == 0 ) {
            return false;
        }

        long nodeCounts = 0l;
        // Hidden Parameter 이름이 Name 이고, arsenalExecutorId 와 arsenalExecutorName 2건이 존재하면 true.
        if ((nodeCounts = IntStream.range(0, hiddenParameterDefinitions.getLength())
                    .mapToObj(hiddenParameterDefinitions::item)
                    .filter(Element.class::isInstance)
                    .map(Element.class::cast)
                    .flatMap(m -> filterNode(m.getChildNodes(), "name").stream())
                    .map(m -> m.getTextContent().equals(ARSENAL_EXECUTOR_FIELD_ID) || m.getTextContent().equals(ARSENAL_EXECUTOR_FIELD_NAME))
                    .count()) >= 2) {
            log.info(" -- Hidden Parameter Counts : {}", nodeCounts);
            return true;
        }

        // 그 외에는 false
        return false;
    }

    private String addArsenalHiddenParameterMapConfig(Document document) {

        try {

            // Arsenal Hidden Parameter 가 이미 있는 경우 현재 Document를 XML String 으로 반환함.
            if ( isExistArsenalHiddenParameterMap(document) ) {
                return ConstUtil.getXmlString(document);
            }

            // "arsenalExecutorId" 에 대한 HiddenParameter 생성
            Element userIdHiddenElement = document.createElement("com.wangyin.parameter.WHideParameterDefinition");

            Element userIdElement       = document.createElement("name");
            Element userIdDescription   = document.createElement("description");
            Element userIdDefaultValue  = document.createElement("defaultValue");

            userIdElement.setTextContent(ARSENAL_EXECUTOR_FIELD_ID);
            userIdDescription.setTextContent("Arsenal 사용자 계정");

            userIdHiddenElement.appendChild(userIdElement);
            userIdHiddenElement.appendChild(userIdDescription);
            userIdHiddenElement.appendChild(userIdDefaultValue);


            // "arsenalExecutorName" 에 대한 HiddenParameter 생성
            Element userNameHiddenElement = document.createElement("com.wangyin.parameter.WHideParameterDefinition");

            Element userNameElement       = document.createElement("name");
            Element userNameDescription   = document.createElement("description");
            Element userNameDefaultValue  = document.createElement("defaultValue");

            userNameElement.setTextContent(ARSENAL_EXECUTOR_FIELD_NAME);
            userNameDescription.setTextContent("Arsenal 사용자 명칭");

            userNameHiddenElement.appendChild(userNameElement);
            userNameHiddenElement.appendChild(userNameDescription);
            userNameHiddenElement.appendChild(userNameDefaultValue);


            // ParameterDefinitions 하위에 신규 2건을 추가함.
            NodeList parameterDefinitions = document.getElementsByTagName("parameterDefinitions");

            parameterDefinitions.item(0).appendChild(userIdHiddenElement);
            parameterDefinitions.item(0).appendChild(userNameHiddenElement);

            return ConstUtil.getXmlString(document);

        } catch (TransformerException e) {
            log.warn("      -- [XML Document를 String 으로 전환하는 과정에서 오류가 발생되었습니다.] --", e);
            throw new CustomArsenalException(CustomStatusCd.CD_500.getCd(), CustomStatusCd.CD_500.getMsg());
        }
    }


    /**
     * Pipeline XML을 읽어 Parameter를 찾는 기능.
     *
     * @param document
     * @return
     */
    private Map<String, Object> getParameterMap(Document document) {

        NodeList parameterDefinitions = document.getElementsByTagName("parameterDefinitions");

        return
                IntStream.range(0, parameterDefinitions.getLength())
                        .mapToObj(parameterDefinitions::item)
                        .filter(Element.class::isInstance)
                        .map(Element.class::cast)
                        .flatMap(m -> filterNode(m.getChildNodes(), "ParameterDefinition").stream())
                        .map(m -> nameValue(m))
                        .collect(Collectors.toMap(
                                nameValue -> nameValue.getName(),
                                nameValue -> nameValue.getValue(),
                                (m1, m2) -> m1.equals(m2) ? m1 : m1     // Key 중복이 발생된 경우 value m1, m2 중 m1 을 사용한다. 동일한 parameter Key에 value는 첫번째를 취득하는 의미.
                        ));
    }





    /**
     * Jenkins Job(Item)에 해당하는 Gitlab Project 정보 식별
     * @param document
     * @param gitlabResources
     * @return
     */
    private GitProjectInfo getGitProjectInfo(Document document, List<GitlabResource> gitlabResources) {

        NodeList userRemoteConfigNodes = document.getElementsByTagName("hudson.plugins.git.UserRemoteConfig");

        for (int i = 0; i < userRemoteConfigNodes.getLength(); i++) {
            Element parameter = (Element) userRemoteConfigNodes.item(i);
            Element parameterUrl = (Element) parameter.getElementsByTagName("url").item(0);

            String gitlabUri = parameterUrl.getTextContent().trim();
            log.debug("  ----[gitlabUri:{}]", gitlabUri);

            // url을 git.cz.xxx 등 "."으로 시작하는 포인트 부터 검색해서, 처음부터 "/" 최초 지점까지만 URL로 짜름.
            String gitUrl = gitlabUri.substring(0, gitlabUri.indexOf("/", gitlabUri.indexOf(".")));
            log.debug("  ----[gitUrl:{}]", gitUrl);

            // url에서 마지막 "/" 부터 ".git" 앞에까지 문자열을 project 명칭으로 인식.
            String gitProjectName = gitlabUri.substring(gitlabUri.lastIndexOf("/") + 1, gitlabUri.indexOf(".git"));
            log.debug("  ----[gitProjectName:{}]", gitProjectName);

            // gitUrl을 앞에서 부터 다시 짜르고, 뒤에서 gitProjectName까지 중간을 얻음.
            String gitGroupName = gitlabUri.substring(gitUrl.length() + 1, gitlabUri.lastIndexOf(gitProjectName) - 1);
            // subGroup과 같이 중간에 "/"이 존재하면, 마지막 group 명칭으로 획득
            gitGroupName = gitGroupName.indexOf("/") > 0 ? gitGroupName.substring(gitGroupName.indexOf("/") + 1) : gitGroupName;
            log.debug("  ----[gitGroupName:{}]", gitGroupName);

            // url를 domain만 짜른것과 같이 gitlab devtoolsId에 있는 /api/v4 정보도 날리고 domain만 획득 해서 비교함.
            Long devToolsId = gitlabResources.stream()
                    .filter(f -> f.getUrl().substring(0, f.getUrl().indexOf("/", f.getUrl().indexOf("."))).equals(gitUrl))
                    .findFirst()
                    .map(GitlabResource::getDevToolsId)
                    .orElse(0L);
            log.debug("  ----[devToolsId:{}]", devToolsId);

            // Git 주소 정보가 다수가 존재할 수 있으나, arsenal template에 의해 만들어 진 경우, 첫번째 프로젝트에 한해서 자신에 대한 내용임.
            return GitProjectInfo.builder()
                    .url(gitUrl)
                    .groupName(gitGroupName)
                    .projectName(gitProjectName)
                    .devToolsId(devToolsId)
                    .build();
        }

        return GitProjectInfo.builder().build();
    }


    /**
     * Jenkins pipeline 구성 configxml 조회
     * @param resource
     * @param jobUrl
     * @return
     */
    private String getJenkinsItemConfigXmlString(JenkinsResource resource, String jobUrl) {
        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(resource.getAccessToken(), resource.getAccount()));

        ResponseEntity<String> responseEntity = (ResponseEntity<String>) jenkinsService.callJenkinsServer(
                jobUrl + "/config.xml",
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        return responseEntity.getBody();
    }

    /**
     * Convert Jenkins XML to XML Document
     * @param configXml
     * @return
     */
    private Document getJenkinsConfigDocument(String configXml) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(configXml));

            return builder.parse(inputSource);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.warn("      -- [Job ProjectName 명칭을 확인중에 오류가 발생되었습니다만, 해당 내용은 무시합니다.] --");
        }

        return null;
    }
}
