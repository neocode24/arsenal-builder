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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Arsenal Dev GitLab GroupService
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2019-06-25
 */

@Slf4j
@Service
public class GitLabGroupService {


    @Autowired
    private GitLabMemberService memberService;

    @Autowired
    private GitLabService gitLabService;


    /**
     * GitLab Group 생성
     * 사용자는 이미 생성되어 있어야 함.
     *
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param account GitLab Account
     * @param userName GitLab UserName(사번)
     * @param name The name of the group
     * @param path The path of the group
     * @param description The group's description
     * @param visibility The group's visibility. Can be private, internal or public.
     * @param lfsEnabled Enable/disable Large File Storage(LFS) for the porjects in this group
     * @param requestAccessEnabled allow users to request member access.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map<?, ?> createGroupWithExistUser(String token,
                                              String uri,
                                              String account,
                                              String userName,
                                              String name,
                                              String path,
                                              String description,
                                              String visibility,
                                              boolean lfsEnabled,
                                              boolean requestAccessEnabled) {




        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", name);
        body.add("path", path);
        body.add("description", description);
        body.add("visibility", visibility);
        body.add("lfs_enabled", lfsEnabled);
        body.add("request_access_enabled", requestAccessEnabled);

        final HttpEntity<?> requestEntity = new HttpEntity<>(body, gitLabService.createHeader(token));

        ResponseEntity<Map> responseEntity = (ResponseEntity<Map>) gitLabService.callGitLabServer(uri + "/groups", HttpMethod.POST, requestEntity);

        try {
            // Group 생성에 새로운 사용자를 Owner로 지정하고, 기본계정('arsenal-dev')은 제거함.
            memberService.addMemberToGroup(token, uri, userName, name, 50);
            memberService.removeMemberFromGroup(token, uri, account, name);
        } catch ( Exception e ) {
            log.error("GitLab Group 생성 후, Member 추가/제거 과정에서 오류가 발생하였습니다. GitLab Group을 제거 합니다.", e);
            removeGroup(token, uri, name);

            throw new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg());
        }

        return responseEntity.getBody();
    }


    /**
     * GitLab Group 삭제
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param groupName GitLab Group Name
     */
    public void removeGroup(String token,
                            String uri,
                            String groupName) {

        String groupId = getGroupId(token, uri, groupName);
        if ( groupId != null ) {
            final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));

            gitLabService.callGitLabServer(uri + "/groups/" + groupId, HttpMethod.DELETE, requestEntity);
        }
        else {
            log.info("groupId:{} 가 존재하지 않습니다.", groupId);
        }
    }

    /**
     * GitLab Group 조회 및 확인.
     * Group 명이 있으면 Group 정보를 제공
     *
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param name GitLab Group Name
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findGroup(String token, String uri, String name) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));
        Map<String, Object> param = new HashMap<>();
        param.put("name", name);

        ResponseEntity<List<Map<String, Object>>> responseEntity = (ResponseEntity<List<Map<String, Object>>>) gitLabService.callGitLabServer(
                uri + "/groups?search={name}",
                HttpMethod.GET,
                requestEntity,
                List.class,
                param);
        return responseEntity.getBody().stream().filter(f -> name.equals(f.get("name"))).collect(Collectors.toList());
    }

    /**
     * GitLab의 모든 Group 이름 정보 획득.
     * Gitlab rest api로 기본 group 조회 시에는 paging 이 포함되어 있기에 999_999 로 강제 지정함.
     * @param token
     * @param uri
     * @return
     */
    public List<Map<String, Object>> findAllGroupNames(String token, String uri) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));

        ResponseEntity<List<Map<String, Object>>> responseEntity = (ResponseEntity<List<Map<String, Object>>>) gitLabService.callGitLabServer(
                uri + "/groups?all_available=true&statistics=true&per_page=999999",
                HttpMethod.GET,
                requestEntity,
                List.class);
        return responseEntity.getBody();
    }


    /**
     * GitLab Group 에 포함된 프로젝트 리스트 조회
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param groupName GitLab Group Name
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findProjectsUnderGroup(String token, String uri, String groupName) {
        // GroupID 확인
        String groupId = Optional.ofNullable(getGroupId(token, uri, groupName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));

        ResponseEntity<List<Map<String, Object>>> responseEntity = (ResponseEntity<List<Map<String, Object>>>) gitLabService.callGitLabServer(
                uri + "/groups/" + groupId + "/projects",
                HttpMethod.GET,
                requestEntity,
                List.class);
        return responseEntity.getBody();
    }


    /**
     * GitLab Group ID 조회
     * @param token GitLab Token
     * @param token GitLab Uri
     * @param groupName GitLab Group Name
     * @return
     */
    public String getGroupId(String token, String uri, String groupName) {
        List<Map<String, Object>> groupList = null;
        if ( (groupList = findGroup(token, uri, groupName)).size() <= 0 ) {
            log.error("GitLab Group이 확인되지 않습니다.");
            return null;
        } else {
            return String.valueOf(groupList.get(0).get("id"));
        }
    }
}
