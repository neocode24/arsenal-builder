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
 * Arsenal Dev GitLab UserService
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2019-06-25
 */

@Slf4j
@Service
public class GitLabUserService {


    @Autowired
    private GitLabService gitLabService;


    /**
     * GitLab User 생성
     *
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param userName GitLab User Name 한글 사용자 명칭
     * @param name GitLab User 사용자 계쩡
     * @param password GitLab 비밀번호
     * @param email GitLab 이메일
     * @return Response Map
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map<?, ?> createUser(String token,
                                String uri,
                                String userName,
                                String name,
                                String password,
                                String email,
                                boolean skipConfirmation) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("username", userName);
        body.add("name", name);
        body.add("password", password);
        body.add("email", email);
        body.add("skip_confirmation", skipConfirmation);

        final HttpEntity<?> requestEntity = new HttpEntity<>(body, gitLabService.createHeader(token));

        ResponseEntity<Map> responseEntity = (ResponseEntity<Map>) gitLabService.callGitLabServer(uri + "/users", HttpMethod.POST, requestEntity);
        return responseEntity.getBody();
    }

    /**
     * GitLab 사용자 삭제
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param userName GitLab UserName
     */
    public void removeUser(String token,
                           String uri,
                           String userName) {
        // userID 확인
        String userId = Optional.ofNullable(getUserId(token, uri, userName)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));

        gitLabService.callGitLabServer(uri + "/users/" + userId, HttpMethod.DELETE, requestEntity);
    }

    /**
     * GitLab 사용자 조회 및 확인
     * 사용자가 있으면 사용자 조회 정보를 제공
     *
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param userName GitLab User Name
     * @return Response List
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findUser(String token, String uri, String userName) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));
        Map<String, Object> param = new HashMap<>();
        param.put("userName", userName);

        ResponseEntity<List<Map<String, Object>>> responseEntity = (ResponseEntity<List<Map<String, Object>>>) gitLabService.callGitLabServer(
                uri + "/users?username={userName}",
                HttpMethod.GET,
                requestEntity,
                List.class,
                param);
        return responseEntity.getBody().stream().filter(e -> userName.equals(e.get("username"))).collect(Collectors.toList());
    }

    /**
     * GitLab 사용자 ID 조회
     * @param token GitLab Token
     * @param uri GitLab Uri
     * @param userName GitLab User Name
     * @return 사용자 ID
     */
    public String getUserId(String token, String uri, String userName) {
        List<Map<String, Object>> userList = null;
        if ( (userList = findUser(token, uri, userName)).size() <= 0 ) {
            log.error("GitLab 사용자가 확인되지 않습니다.");
            return null;
        } else {
            return String.valueOf(userList.get(0).get("id"));
        }
    }
}
