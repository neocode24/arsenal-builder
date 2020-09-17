package com.kt.arsenal.biz.builder.service;

import com.kt.arsenal.biz.autoenvmgmt.service.GitLabMilestoneService;
import com.kt.arsenal.biz.autoenvmgmt.service.GitlabVersionService;
import com.kt.arsenal.biz.autoenvmgmt.service.JenkinsBuildService;
import com.kt.arsenal.biz.builder.domain.BuilderJobRequestDomain;
import com.kt.arsenal.biz.builder.domain.JenkinsResource;
import com.kt.arsenal.biz.builder.domain.LastBuildInfo;
import com.kt.arsenal.common.exception.CustomArsenalException;
import com.kt.arsenal.common.exception.CustomStatusCd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kt.arsenal.biz.autoenvmgmt.service.JenkinsViewService.ARSENAL_EXECUTOR_FIELD_ID;
import static com.kt.arsenal.biz.autoenvmgmt.service.JenkinsViewService.ARSENAL_EXECUTOR_FIELD_NAME;

/**
 * Arsenal-Dev Build를 위한 Service
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/09/07
 */

@Slf4j
@Service
public class BuilderService {

    @Autowired
    private GitlabVersionService gitlabVersionService;

    @Autowired
    private GitLabMilestoneService gitLabMilestoneService;

    @Autowired
    private JenkinsBuildService jenkinsBuildService;


    // Jenkins Parameter 항목중에 TAG 정보를 획득하지 않아도 되는 기본 옵션의 Parameter. 추가 된다면, 늘려야 한다.
    List<String> ignoreParamKeys = Arrays.asList("globalIstioAutoInject", "isForceUpgrade", "freshStart", "yarnEnable", "unitTestEnable", "seleniumTestEnable", ARSENAL_EXECUTOR_FIELD_ID, ARSENAL_EXECUTOR_FIELD_NAME);



    public LastBuildInfo getLastBuildInfo(BuilderJobRequestDomain requestDomain) {

        // 사용자 ServiceAlias 중에 JobInfo에 일치하는 Jenkins 접근 정보 획득
        JenkinsResource jenkinsResource = getJenkinsInfo(requestDomain);

        Map<String, Object> lastBuildStatus = jenkinsBuildService.lastBuildStatus(
                jenkinsResource.getAccessToken(),
                jenkinsResource.getUrl(),
                jenkinsResource.getAccount(),
                requestDomain.getJobInfo().getName()
        );

        Optional.ofNullable(lastBuildStatus.get("number")).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_51008.getCd(), CustomStatusCd.CD_51008.getMsg()));

        // Job 실행에 사용한 실제 Running ParameterMap 획득
        Map<String, Object> parameterMap =
                Optional.ofNullable( (List<Map<String, Object>>)lastBuildStatus.get("actions") ).map(Collection::stream).orElse(Stream.empty())                                 // actions 결과는 List. 없는 경우 Empty Stream
                        .filter(action -> action.get("parameters") != null)                                                                                                     // action 중에 "parameters" List 획득.

                        .flatMap(action -> ((List<Map<String, Object>>)action.get("parameters")).stream().filter(f -> ((String)f.get("_class")).endsWith("ParameterValue")))    // "ParameterValue" 유형만 선별.
                        .collect(Collectors.toMap(
                                parameter -> (String) parameter.get("name"),                        // "ParameterValue 유형에서 name을 key로 인식
                                parameter -> parameter.get("value"),                                // "ParameterValue 유형에서 value를 value로 인식
                                (m1, m2) -> m1.equals(m2) ? m2 : m1                                 // Key 중복이 발생된 경우 value m1, m2 중 m1 을 사용한다. 동일한 parameter Key에 value는 첫번째를 취득하는 의미.
                        ));

        // Job 실행 사용자 정보 획득
        Map<String, Object> causeMap =
                Optional.ofNullable( (List<Map<String, Object>>)lastBuildStatus.get("actions") ).map(Collection::stream).orElse((Stream.empty()))                              // actions 결과는 List. 없는 경우 Empty Stream
                        .filter(cause -> cause.get("causes") != null)                                                                                                           // action 중에 "cause" List 획득.

                        .flatMap(cause -> ((List<Map<String, Object>>)cause.get("causes")).stream().filter(f -> ((String)f.get("_class")).endsWith("UserIdCause")))             // "UserIdCause" 유형만 선별.
                        .findFirst().orElseGet(HashMap::new);
        causeMap.remove("shortDescription");                                                // 불필요한 Key 제거
        causeMap.remove("_class");

        return
                LastBuildInfo.builder()
                        .name(requestDomain.getJobInfo().getName())
                        .url((String) lastBuildStatus.get("url"))
                        .number((Integer) lastBuildStatus.get("number"))
                        .building((Boolean) lastBuildStatus.get("building"))
                        .result((String) lastBuildStatus.get("result"))
                        .timestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli((Long) lastBuildStatus.get("timestamp")), TimeZone.getDefault().toZoneId()))
                        .buildParameterMap(parameterMap)
                        .causeUserId((String) causeMap.get("userId"))
                        .causeUserName((String) causeMap.get("userName"))
                        .duration((Integer) lastBuildStatus.get("duration"))
                        .estimatedDuration((Integer) lastBuildStatus.get("estimatedDuration"))
                        .build();
    }


    /**
     * Jenkins Job 종료 기능
     * @param requestDomain
     */
    public void stopBuild(BuilderJobRequestDomain requestDomain) {

        LastBuildInfo lastBuildInfo = getLastBuildInfo(requestDomain);

        if ( !lastBuildInfo.isBuilding() ) {
            log.info("빌드/배포가 수행중이지 않습니다.");
            throw new CustomArsenalException(CustomStatusCd.CD_51009.getCd(), CustomStatusCd.CD_51009.getMsg());
        }

        // 사용자 ServiceAlias 중에 JobInfo에 일치하는 Jenkins 접근 정보 획득
        JenkinsResource jenkinsResource = getJenkinsInfo(requestDomain);

        boolean isStoped = false;

        try {
            isStoped = jenkinsBuildService.stopBuildItem(
                    jenkinsResource.getAccessToken(),
                    jenkinsResource.getUrl(),
                    jenkinsResource.getAccount(),
                    requestDomain.getJobInfo().getName(),
                    lastBuildInfo.getNumber()
            );

        } catch(HttpClientErrorException e) {
            if ( e.getStatusCode() == HttpStatus.UNAUTHORIZED ) {
                log.warn("빌드 실행중 권한 오류가 발생되었습니다.", e);
                throw new CustomArsenalException(CustomStatusCd.CD_51003.getCd(), CustomStatusCd.CD_51003.getMsg());
            }
            else {
                log.error("Http 4xx 오류가 발생되었습니다.", e);
                throw new CustomArsenalException(CustomStatusCd.CD_51004.getCd(), CustomStatusCd.CD_51004.getMsg());
            }
        } catch(Exception e) {
            log.error("Jenkins Stop 수행중 오류 발생.", e);
            throw new CustomArsenalException(CustomStatusCd.CD_500.getCd(), CustomStatusCd.CD_500.getMsg());
        }

        // 호출 실패일 경우 오류 조치 함. 반복 실행해도 오류되는 것은 아니고, isBuilding 여부로 판단됨.
        if ( !isStoped ) {
            log.warn("배포 중단 요청이 실패 되었습니다. requestDomain:{}", requestDomain);
            throw new CustomArsenalException(CustomStatusCd.CD_51007.getCd(), CustomStatusCd.CD_51007.getMsg());
        }

        log.debug("{}", jenkinsResource);


    }


    /**
     * Jenkins Job 수행 기능
     * @param requestDomain
     */
    public void doBuild(BuilderJobRequestDomain requestDomain) {

        // 동시성 빌드 제약. 같은 Job이 이미 Build 중이라면 예외 처리 함.
        LastBuildInfo lastBuildInfo;
        if ((lastBuildInfo = getLastBuildInfo(requestDomain)).isBuilding() ) {
            log.info("빌드가 진행중이며, 빌드 요청은 무시합니다.", lastBuildInfo);
            throw new CustomArsenalException(CustomStatusCd.CD_51006.getCd(), CustomStatusCd.CD_51006.getMsg());
        }

        // Job 빌드 Parameter
        List<String> params =
        requestDomain.getJobInfo().getParameterDefinitionMap().entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.toList());

        // 사용자 ServiceAlias 중에 JobInfo에 일치하는 Jenkins 접근 정보 획득
        JenkinsResource jenkinsResource = getJenkinsInfo(requestDomain);

        boolean isFired = false;

        try {
            isFired = jenkinsBuildService.buildItemWithParameters(
                    jenkinsResource.getAccessToken(),
                    jenkinsResource.getUrl(),
                    jenkinsResource.getAccount(),
                    requestDomain.getJobInfo().getName(),
                    String.join("&", params));
        } catch(HttpClientErrorException e) {
            if ( e.getStatusCode() == HttpStatus.UNAUTHORIZED ) {
                log.warn("빌드 실행중 권한 오류가 발생되었습니다.", e);
                throw new CustomArsenalException(CustomStatusCd.CD_51003.getCd(), CustomStatusCd.CD_51003.getMsg());
            }
            else {
                log.error("Http 4xx 오류가 발생되었습니다.", e);
                throw new CustomArsenalException(CustomStatusCd.CD_51004.getCd(), CustomStatusCd.CD_51004.getMsg());
            }
        } catch(Exception e) {
            log.error("Jenkins Build 수행중 오류 발생.", e);
            throw new CustomArsenalException(CustomStatusCd.CD_500.getCd(), CustomStatusCd.CD_500.getMsg());
        }

        // 호출 실패일 경우 오류 조치 함. 반복 실행해도 오류되는 것은 아니고, isBuilding 여부로 판단됨.
        if ( !isFired ) {
            log.warn("배포 요청이 실패 되었습니다. requestDomain:{}", requestDomain);
            throw new CustomArsenalException(CustomStatusCd.CD_51007.getCd(), CustomStatusCd.CD_51007.getMsg());
        }
    }


    /**
     * 요청된 requestDomain 정보에서 Job 정보에 있는 ParameterMap 정보의 Value를 다시 조회함.
     * ParameterMap에서 이미 존재하는 값은 Jenkins에서 조회된 기본값의 의미 임.
     * 기본값 자체가 Jenkins에서 Gitlab으로 확인한 값이긴 하지만, 다른 값을 선택할 수 있도록 Gitlab에서 다시 조회 하여 Value를 List로 작성함.
     *
     * @param requestDomain
     */
    public void setBuildParameters(BuilderJobRequestDomain requestDomain) {

        // 초기값이 포함된 ParameterMap를 Loop 하면서, 필요한 값을 조회하여 변경함.
        requestDomain.getJobInfo().getParameterDefinitionMap().entrySet().forEach(entry -> {
            String key = (String) entry.getKey();

            String jenkinsJobName   = requestDomain.getJobInfo().getName();
            String gitAccessToken   = getGitlabToken(requestDomain, requestDomain.getJobInfo().getGitDevToolsId());
            String gitUrl           = requestDomain.getJobInfo().getGitUrl() + "/api/v4";
            String gitGroupName     = requestDomain.getJobInfo().getGitGroupName();
            String gitProjectName   = requestDomain.getJobInfo().getGitProjectName();

            // 기본값 처리
            if ( ignoreParamKeys.contains(key) ) {
                log.debug("Branch나 Tag 정보에 Parameter는 기본값으로 사용합니다. [parameberKey:{}, parameterValue:{}]", key, entry.getValue());
            }

            // parameter - "branchName" : Jenkins에 연관된 Gitlab 프로젝트 정보 조회. 없는 경우 오류 발생. (Pipeline에서 Gitlab 정보가 일치하지 않는 경우)
            // TAG, -SIT, -PRD-DEPLOY 와 같이 개발환경이 아는 배포를 하는 경우 master 에서 빌드하는 경우를 제외 함.
            // release- 로 시작하는 브랜치만 사용 가능.
            // Jenkins 사용하듯이 "origin"을 붙여서 branch 명칭을 생성함.
            else if ( "branchName".equals(key) ) {
                try {
                    List<String> branches = gitlabVersionService.getBranches(gitAccessToken, gitUrl, gitGroupName, gitProjectName);

                    if ( jenkinsJobName.indexOf("-PRD-DEPLOY") > -1 || jenkinsJobName.indexOf("-SIT-DEPLOY") > -1 || jenkinsJobName.indexOf("-TAG") >-1 ) {
                        branches = branches.stream().filter(f -> !"master".equals(f)).filter(f -> f.indexOf("release-") > -1).collect(Collectors.toList());
                    }

                    // 배포를 위한 branch 가 없는 경우 오류 발생. PRD, SIT, TAG는 Master를 제외한 branch 수. DOCKERIZE는 master를 포함한 전체 Branch
                    if ( branches.size() < 1 ) {
                        throw new CustomArsenalException(CustomStatusCd.CD_51005.getCd(), CustomStatusCd.CD_51005.getMsg());
                    }

                    entry.setValue(branches.stream().map(m -> "origin/" + m).collect(Collectors.toList()));

                } catch (NoSuchElementException e) {
                    throw new CustomArsenalException(CustomStatusCd.CD_51001.getCd(), "[" + gitProjectName + "] " + CustomStatusCd.CD_51001.getMsg());
                }
            }

            // parameter - "version" : SIT, PRD 배포를 위한 Version. 즉 Gitlab Milestone이 Active 된 항목이 있어야 하나, 없다면, 배포가 불가능 함. 오류 안내 사항.
            else if ( "version".equals(key) ) {

                List<GitLabMilestoneService.MilestoneInfo> activeMilestones
                        = gitLabMilestoneService.getActiveMilestones(gitAccessToken, gitUrl, gitGroupName, gitProjectName);

                if (!Optional.ofNullable(activeMilestones).isPresent() || activeMilestones.size() == 0) {
                    throw new CustomArsenalException(CustomStatusCd.CD_51002.getCd(), "[" + gitProjectName + "] " + CustomStatusCd.CD_51002.getMsg());
                }

                entry.setValue(activeMilestones.stream().map(GitLabMilestoneService.MilestoneInfo::getTitle).collect(Collectors.toList()));
            }

            // 그 외 Parameter 명칭은은 배포에 포함될 프로젝트 명칭들이며, 모두 TAG를 거쳐서 생성된 tag 만 획득함.
            // arsenal-dev, arsenal-sit, arsenal-prd 프로젝트는 배포 이력을 가진 프로젝트이며, 해당 프로젝트 배포 원복을 위해 선택 하는 값임
            else {
                try {

                    entry.setValue(gitlabVersionService.getTags(gitAccessToken, gitUrl, gitGroupName, key));
                } catch (Exception e) {
                    log.warn("Parameter Key - [{}] 정보롤 Tag를 조회 하는 중에 오류가 발생하였습니다. Tag에 대한 Parameter가 아닐 수 있으며, 기본값을 대체 합니다.", key, e);
                }
            }

        });
    }


    /**
     * Gitlab Token을 찾는 함수
     * @param requestDomain
     * @return
     */
    private String getGitlabToken(BuilderJobRequestDomain requestDomain, Long gitDevToolsId) {
        return
                requestDomain.getGitlabResources().stream()
                    .filter(f -> f.getDevToolsId().equals(gitDevToolsId))
                    .findFirst()
                    .map(f -> f.getAccessToken())
                    .orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_500.getCd(), CustomStatusCd.CD_500.getMsg()));
    }

    /**
     * Jenkins Token을 찾는 함수
     * @param requestDomain
     * @return
     */
    private JenkinsResource getJenkinsInfo(BuilderJobRequestDomain requestDomain) {

        String jenkinsUrl = requestDomain.getJobInfo().getUrl();
        String jenkinsDomain = jenkinsUrl.substring(0, jenkinsUrl.indexOf("/", jenkinsUrl.indexOf(".")));

        return
                requestDomain.getJenkinsResources().stream()
                    .filter(f -> f.getUrl().equals(jenkinsDomain))
                    .findFirst()
                    .orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_500.getCd(), CustomStatusCd.CD_500.getMsg()));
    }
}
