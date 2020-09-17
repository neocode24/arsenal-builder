package com.kt.arsenal.biz.autoenvmgmt.service;

import com.kt.arsenal.common.exception.CustomArsenalException;
import com.kt.arsenal.common.exception.CustomStatusCd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Arsenal Dev GitLab MemberService
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2019-06-25
 */

@Slf4j
@Service
public class GitLabMemberService {


    @Autowired
    private GitLabGroupService groupService;

    @Autowired
    private GitLabProjectService projectService;

    @Autowired
    private GitLabUserService userService;

    @Autowired
    private GitLabService gitLabService;


    /**
     * Group에 사용자 추가
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param userName GitLab UserName
     * @param groupName GitLab Group Name
     * @param accessLevel 10 - Guest, 20 - Reporter, 30 - Developer, 40 - Maintainer, 50 - Owner
     */
    @SuppressWarnings({ "unchecked", "rawtypes", "unused" })
    public void addMemberToGroup(String token, String uri, String userName, String groupName, int accessLevel) {
        // GroupID 확인
        String groupId = Optional.ofNullable(groupService.getGroupId(token, uri, groupName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        // 사용자ID 확인
        String userId = Optional.ofNullable(userService.getUserId(token, uri, userName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));


        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", groupId);
        body.add("user_id", userId);
        body.add("access_level", accessLevel);

        final HttpEntity<?> requestEntity = new HttpEntity<>(body, gitLabService.createHeader(token));

        ResponseEntity<Map> responseEntity = (ResponseEntity<Map>) gitLabService.callGitLabServer(uri + "/groups/" + groupId + "/members", HttpMethod.POST, requestEntity);
    }


    /**
     * Group에 사용자 제거
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param userName GitLab UserName
     * @param groupName GitLab Group Name
     */
    @SuppressWarnings({ "unchecked", "unused", "rawtypes" })
    public void removeMemberFromGroup(String token, String uri, String userName, String groupName) {
        // GroupID 확인
        String groupId = Optional.ofNullable(groupService.getGroupId(token, uri, groupName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        // 사용자ID 확인
        String userId = Optional.ofNullable(userService.getUserId(token, uri, userName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));


        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", groupId);
        body.add("user_id", userId);

        final HttpEntity<?> requestEntity = new HttpEntity<>(body, gitLabService.createHeader(token));

        ResponseEntity<Map> responseEntity = (ResponseEntity<Map>) gitLabService.callGitLabServer(uri + "/groups/" + groupId + "/members/" + userId, HttpMethod.DELETE, requestEntity);
    }



    /**
     * Project에 사용자 추가
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param userName Gitlab UserName
     * @param gitlabGroupName Gitlab GroupName
     * @param gitProjectName GitLab Project Name
     * @param accessLevel 10 - Guest, 20 - Reporter, 30 - Developer, 40 - Maintainer, 50 - Owner
     */
    @SuppressWarnings({ "unchecked", "rawtypes", "unused" })
    public void addMemberToProject(String token, String uri, String userName, String gitlabGroupName, String gitProjectName, int accessLevel) {
        // Gitlab ProjectID 확인
        String gitProjectId = Optional.ofNullable(projectService.getGitProjectId(token, uri, gitlabGroupName, gitProjectName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        // 사용자ID 확인
        String userId = Optional.ofNullable(userService.getUserId(token, uri, userName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));


        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", gitProjectId);
        body.add("user_id", userId);
        body.add("access_level", accessLevel);

        final HttpEntity<?> requestEntity = new HttpEntity<>(body, gitLabService.createHeader(token));

        ResponseEntity<Map> responseEntity = (ResponseEntity<Map>) gitLabService.callGitLabServer(uri + "/projects/" + gitProjectId + "/members", HttpMethod.POST, requestEntity);
    }

    /**
     * Group에 사용자 전체 조회
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param groupName GitLab Group Name
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listMemberFromGroup(String token, String uri, String groupName) {
        return listMemberFromGroup(token, uri, groupName, false);
    }

    /**
     * Group에 사용자 전체 조회
     * @param token
     * @param uri
     * @param groupName
     * @param includeInherited
     * @return
     */
    public List<Map<String, Object>> listMemberFromGroup(String token, String uri, String groupName, boolean includeInherited) {

        String includeStr = includeInherited ? "/members/all" : "/members";

        // GroupID 확인
        String groupId = Optional.ofNullable(groupService.getGroupId(token, uri, groupName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));

        ResponseEntity<List<Map<String, Object>>> responseEntity = (ResponseEntity<List<Map<String, Object>>>) gitLabService.callGitLabServer(
                uri + "/groups/" + groupId + includeStr,
                HttpMethod.GET,
                requestEntity,
                List.class);

        return responseEntity.getBody();
    }



    /**
     * Project에 사용자 전체 조회
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param gitGroupName GitLab Group Name
     * @param gitProjectName GitLab Project Name
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listMemberFromProject(String token, String uri, String gitGroupName, String gitProjectName) {
        return listMemberFromProject(token, uri, gitGroupName, gitProjectName, false);
    }

    /**
     * Project에 사용자 전체 조회. 상속권한자 포함.
     * @param token
     * @param uri
     * @param gitlabGroupName
     * @param gitProjectName
     * @param includeInherited
     * @return
     */
    public List<Map<String, Object>> listMemberFromProject(String token, String uri, String gitlabGroupName, String gitProjectName, boolean includeInherited) {

        log.debug("listMember - token:[{}], uri:[{}], gitlabGroupName:[{}], gitProjectName:[{}], includeInherited:[{}]",
                token, uri, gitlabGroupName, gitProjectName, includeInherited);

        String includeStr = includeInherited ? "/members/all" : "/members";

        // ProjectID 확인
        String gitProjectId = Optional.ofNullable(projectService.getGitProjectId(token, uri, gitlabGroupName, gitProjectName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));

        ResponseEntity<List<Map<String, Object>>> responseEntity = (ResponseEntity<List<Map<String, Object>>>) gitLabService.callGitLabServer(
                uri + "/projects/" + gitProjectId + includeStr,
                HttpMethod.GET,
                requestEntity,
                List.class);

        return responseEntity.getBody();
    }

    /**
     * Project에서 지정된 한명을 제외하고 모두 멤버에서 제외 함.
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param account GitLab Account
     * @param existUserName GitLab User Name
     * @param gitlabGroupName Gitlab Group Name
     * @param gitProjectName GitLab Project Name
     * @param accessLevel 10 - Guest, 20 - Reporter, 30 - Developer, 40 - Maintainer, 50 - Owner
     */
    @SuppressWarnings("unused")
    public void clearMemberFromProjectButOne(String token, String uri, String account, String existUserName, String gitlabGroupName, String gitProjectName, int accessLevel) {
        // ProjectID 확인
        String gitProjectId = Optional.ofNullable(projectService.getGitProjectId(token, uri, gitlabGroupName, gitProjectName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        // 사용자ID 확인
        String userId = Optional.ofNullable(userService.getUserId(token, uri, existUserName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        // arsenal-dev 자동화 계정을 제외하고 모두 멤버제외 처리함.
        listMemberFromProject(token, uri, gitlabGroupName, gitProjectName).stream().filter(f -> !f.get("username").equals(account)).forEach(m -> {
            String userName = String.valueOf(m.get("username"));

            log.debug("사용자 ID [{}]를 프로젝트:[{}]에서 제외 합니다.", userName, gitProjectName);
            removeMemberFromProject(token, uri, userName, gitlabGroupName, gitProjectName);
        });

        addMemberToProject(token, uri, existUserName, gitlabGroupName, gitProjectName, accessLevel);
    }

    /**
     * Project에서 모든 멤버를 제외함. Owner는 삭제되지 않음.
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param account GitLab Account
     * @param gitlabGroupName Gitlab GroupName
     * @param gitProjectName GitLab Project Name
     */
    @SuppressWarnings("unused")
    public void clearMemberFromProject(String token, String uri, String account, String gitlabGroupName, String gitProjectName) {
        // ProjectID 확인
        String gitProjectId = Optional.ofNullable(projectService.getGitProjectId(token, uri, gitlabGroupName, gitProjectName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        // arsenal-dev 자동화 계정을 제외하고 모두 멤버제외 처리함.
        listMemberFromProject(token, uri, gitlabGroupName, gitProjectName).stream().filter(f -> !f.get("username").equals(account)).forEach(m -> {
            String userName = String.valueOf(m.get("username"));

            log.debug("사용자 ID [{}]를 프로젝트:[{}]에서 제외 합니다.", userName, gitProjectName);
            removeMemberFromProject(token, uri, userName, gitlabGroupName, gitProjectName);
        });
    }


    /**
     * Project에 사용자 제거
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param userName GitLab UserName
     * @param gitlabGroupName Gitlab GroupName
     * @param gitProjectName GitLab Project Name
     */
    @SuppressWarnings({ "unchecked", "rawtypes", "unused" })
    public void removeMemberFromProject(String token, String uri, String userName, String gitlabGroupName, String gitProjectName) {
        // ProjectID 확인
        String gitProjectId = Optional.ofNullable(projectService.getGitProjectId(token, uri, gitlabGroupName, gitProjectName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        // 사용자ID 확인
        String userId = Optional.ofNullable(userService.getUserId(token, uri, userName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));


        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("id", gitProjectId);
        body.add("user_id", userId);

        final HttpEntity<?> requestEntity = new HttpEntity<>(body, gitLabService.createHeader(token));

        ResponseEntity<Map> responseEntity = (ResponseEntity<Map>) gitLabService.callGitLabServer(uri + "/projects/" + gitProjectId + "/members/" + userId, HttpMethod.DELETE, requestEntity);
    }
}
