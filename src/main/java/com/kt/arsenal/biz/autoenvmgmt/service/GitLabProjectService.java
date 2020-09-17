package com.kt.arsenal.biz.autoenvmgmt.service;

import com.kt.arsenal.common.exception.CustomArsenalException;
import com.kt.arsenal.common.exception.CustomStatusCd;
import com.kt.arsenal.common.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Arsenal Dev GitLab ProjectService
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2019-06-25
 */

@Slf4j
@Service
public class GitLabProjectService {


    @Autowired
    private GitLabGroupService groupService;

    @Autowired
    private GitLabUserService userService;

    @Autowired
    private GitLabService gitLabService;


    /**
     * GitLab Project 생성
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param userName GitLab UserName(사번)
     * @param groupName GitLab GroupName
     * @param name The name of the new project. Equals path if not provided.
     * @param path Repository name for new project Generated based on name if not provided
     * @param defaultBranch master by default
     * @param description Short project description
     * @param visibility private, internal, public
     * @param lfsEnabled Enable LFS
     * @param requestAccessEnabled Allow users to request member access
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map<?, ?> createProjectWithExistUser(String token,
                                                String uri,
                                                String userName,
                                                String groupName,
                                                String name,
                                                String path,
                                                String defaultBranch,
                                                String description,
                                                String visibility,
                                                boolean lfsEnabled,
                                                boolean requestAccessEnabled) {

        // Project 생성전에 소속할 그룹이 없으면 오류 처리 함.
        String groupId = Optional.ofNullable(groupService.getGroupId(token, uri, groupName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        // Project 생성 소유자를 지정하기 위한 사용자ID 확인
        String userId = Optional.ofNullable(userService.getUserId(token, uri, userName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", name);
        body.add("path", path);
        body.add("default_branch", defaultBranch);
        body.add("description", description);
        body.add("visibility", visibility);
        body.add("lfs_enabled", lfsEnabled);
        body.add("request_access_enabled", requestAccessEnabled);

        body.add("user_id", userId);
        body.add("namespace_id", groupId);

        final HttpEntity<?> requestEntity = new HttpEntity<>(body, gitLabService.createHeader(token));

        ResponseEntity<Map> responseEntity = (ResponseEntity<Map>) gitLabService.callGitLabServer(uri + "/projects", HttpMethod.POST, requestEntity);
        return responseEntity.getBody();
    }

    /**
     * GitLab Group 삭제
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param groupName GitLab GroupName
     * @param projectName GitLab ProjectName
     */
    public void removeProject(String token,
                              String uri,
                              String groupName,
                              String projectName) {
        // ProjectId 확인
        String gitProjectId = Optional.ofNullable(getGitProjectId(token, uri, groupName, projectName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));

        gitLabService.callGitLabServer(uri + "/projects/" + gitProjectId, HttpMethod.DELETE, requestEntity);
    }

    /**
     * Project Commit Count 획득.
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param groupName GitLab GroupName
     * @param name GitLab ProjectName
     * @return
     */
    public Integer getCommitCount(String token, String uri, String groupName, String name) {

        String projectId = getGitProjectId(token, uri, groupName, name);
        if ( projectId == null ) {
            log.info("ProjectId가 존재하지 않아, CommitCount 는 0으로 대체 합니다.");
            return 0;
        }

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));
        Map<String, Object> param = new HashMap<>();
        param.put("statistics", true);

        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) gitLabService.callGitLabServer(
                uri + "/projects/" + projectId + "?statistics={statistics}",
                HttpMethod.GET,
                requestEntity,
                Map.class,
                param);

        Map<String, Object> statisticsMap = (Map<String, Object>) responseEntity.getBody().get("statistics");
        if ( statisticsMap != null ) {
            return statisticsMap.get("commit_count") != null ? (Integer) statisticsMap.get("commit_count") : 0;
        }

        return 0;
    }

    /**
     * Project Last Commit 정보 획득
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param groupName GitLab GroupName
     * @param name GitLab ProjectName
     * @return
     */
    public Map<String, Object> getLastCommitInfo(String token, String uri, String groupName, String name) {

        String projectId = getGitProjectId(token, uri, groupName, name);
        if ( projectId == null ) {
            log.info("ProjectId가 존재하지 않아, Commit 정보는 수집하지 않습니다.");
            return new HashMap<>();
        }

        if ( !isGitProjectAccessable(token, uri, groupName, name) ) {
            log.info("Project가 접근 가능한 상태가 아닙니다. Commit 정보를 수집하지 않습니다.");
            return new HashMap<>();
        }


        // 매 Loop 마다 피보나치 수열(2,3,5,8,13,21,34,55,89) 만큼의 이전 날짜까지 단계적으로 commit 이력을 조회 하여, 최근 이력이 있는 경우 해당까지만 Loop 함.
        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));
        for ( int fibonacci : CommonUtil.getFibonacciLoopSize(10) ) {
            Map<String, Object> param = new HashMap<>();
            param.put("since", LocalDateTime.now().minusDays(fibonacci));


            ResponseEntity<List<Map<String, Object>>> responseEntity = (ResponseEntity<List<Map<String, Object>>>) gitLabService.callGitLabServer(
                    uri + "/projects/" + projectId + "/repository/commits?since={since}",
                    HttpMethod.GET,
                    requestEntity,
                    List.class,
                    param);

            if (responseEntity.getBody().size() > 0 ) {
                log.debug("최근 Commit 정보를 발견하였습니다. Fibonacci value:[{}]", fibonacci);
                return responseEntity.getBody().stream().max(
                                Comparator.comparing(f -> LocalDateTime.parse((String)f.get("created_at"), DateTimeFormatter.ISO_DATE_TIME))).orElse(responseEntity.getBody().get(0));
            }

        }

        return new HashMap<>();

    }

    /**
     * 특정 Group에 속하는 GitLab Project 확인 및 조회.
     * groupName 를 지정하지 않으면, 전체 그룹에서 확인한다.
     *
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param groupName GitLab groupName
     * @param name GitLab ProjectName
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findProject(String token, String uri, String groupName, String name) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));
        Map<String, Object> param = new HashMap<>();
        param.put("name", name);
        param.put("group", groupName);

        StringBuilder apiSubUri = StringUtils.isEmpty(groupName) ?
                new StringBuilder(uri).append("/projects?search={name}").append("&per_page=100") :
                new StringBuilder(uri).append("/groups/{group}").append("/projects?search={name}").append("&per_page=100");

        ResponseEntity<List<Map<String, Object>>> responseEntity = (ResponseEntity<List<Map<String, Object>>>) gitLabService.callGitLabServer(
                apiSubUri.toString(),
                HttpMethod.GET,
                requestEntity,
                Object.class,
                param);

        // Notfound or non-list
        if (responseEntity.getStatusCode() == HttpStatus.NOT_FOUND ||
                !(responseEntity.getBody() instanceof ArrayList)) {
            return new ArrayList<Map<String, Object>>();
        }

        List<Map<String, Object>> returnBody = (List<Map<String, Object>>) responseEntity.getBody();
        if (StringUtils.isEmpty(groupName)) {
            return returnBody.stream().filter(f -> name.equals(f.get("name"))).collect(Collectors.toList());
        } else {
            return returnBody.stream().filter(f ->
                    name.equals(f.get("name")) && groupName.equals(((Map<String, Object>) f.get("namespace")).get("name"))).collect(Collectors.toList());
        }
    }

    /**
     * GitLab Project ID 조회
     * @param token GitLab Token
     * @param token GitLab Uri
     * @param gitProjectName GitLab Project Name
     * @return
     */
    public String getGitProjectId(String token, String uri, String groupName, String gitProjectName) {
        log.debug("Find GitProject : [token:{}], [uri:{}], [groupName:{}], [gitProjectName:{}]", token, uri, groupName, gitProjectName);

        List<Map<String, Object>> projectList = null;
        if ( (projectList = findProject(token, uri, groupName, gitProjectName)).size() <= 0 ) {
            log.debug("GitLab Project가 확인되지 않습니다. Group 지정 없이 다시 확인합니다. groupName:[{}], projectName:[{}]", groupName, gitProjectName);

            if ((projectList = findProject(token, uri, null, gitProjectName)).size() <= 0 ) {
                log.error("GitLab Project가 확인되지 않습니다. projectName:[{}]", gitProjectName);
                return null;
            } else {
                return String.valueOf(projectList.get(0).get("id"));
            }
        } else {
            return String.valueOf(projectList.get(0).get("id"));
        }
    }

    /**
     * GitLab Project 접근 및 사용가능여부 조회
     * @param token GitLab Token
     * @param uri GitLab ri
     * @param gitProjectName GitLab Project Name
     * @return
     */
    public boolean isGitProjectAccessable(String token, String uri, String groupName, String gitProjectName) {
        List<Map<String, Object>> projectList = null;
        if ( (projectList = findProject(token, uri, groupName, gitProjectName)).size() <= 0 ) {
            log.debug("GitLab Project가 확인되지 않습니다. Group 지정 없이 다시 확인합니다. groupName:[{}], projectName:[{}]", groupName, gitProjectName);

            if ((projectList = findProject(token, uri, null, gitProjectName)).size() <= 0 ) {
                log.error("GitLab Project가 확인되지 않습니다. projectName:[{}]", gitProjectName);
                return false;
            }
            return ((boolean)projectList.get(0).get("merge_requests_enabled")) && !StringUtils.isEmpty((CharSequence) ((Map<String, Object>)(projectList.get(0).get("_links"))).get("merge_requests"));
        } else {

            /*
                "_links": {
                    "self": "http://10.217.59.20/api/v4/projects/2073",
                    "issues": "http://10.217.59.20/api/v4/projects/2073/issues",
                    "merge_requests": "http://10.217.59.20/api/v4/projects/2073/merge_requests",            -- 접근 불가일 경우 merge_requests 가 없음.
                    "repo_branches": "http://10.217.59.20/api/v4/projects/2073/repository/branches",
                    "labels": "http://10.217.59.20/api/v4/projects/2073/labels",
                    "events": "http://10.217.59.20/api/v4/projects/2073/events",
                    "members": "http://10.217.59.20/api/v4/projects/2073/members"
                },
                "archived": false,
                "visibility": "internal",
                "resolve_outdated_diff_discussions": false,
                "container_registry_enabled": true,
                "issues_enabled": true,
                "merge_requests_enabled": true,                                                             -- 접근 불가일 경우 false 임.
             */

            return ((boolean)projectList.get(0).get("merge_requests_enabled")) && !StringUtils.isEmpty((CharSequence) ((Map<String, Object>)(projectList.get(0).get("_links"))).get("merge_requests"));
        }
    }

    /**
     * GitLab Project 정보 변경
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param groupName GitLab Group Name
     * @param projectName GitLab Project Name
     * @param description GitLab Project Description
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map<?, ?> editProjectDescription(String token,
                                            String uri,
                                            String groupName,
                                            String projectName,
                                            String description) {

        // ProjectId 확인
        String gitProjectId = Optional.ofNullable(getGitProjectId(token, uri, groupName, projectName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("description", description);

        final HttpEntity<?> requestEntity = new HttpEntity<>(body, gitLabService.createHeader(token));

        ResponseEntity<Map> responseEntity = (ResponseEntity<Map>) gitLabService.callGitLabServer(
                uri + "/projects/" + gitProjectId,
                HttpMethod.PUT,
                requestEntity);

        log.debug("GitLab 프로젝트 설명이 변경되었습니다. ProjectName:[{}]", projectName);
        return responseEntity.getBody();
    }

}
