package com.kt.arsenal.biz.autoenvmgmt.service;

import com.kt.arsenal.common.exception.CustomArsenalException;
import com.kt.arsenal.common.exception.CustomStatusCd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Arsenal-Dev
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/09/07
 */

@Slf4j
@Service
public class GitlabVersionService {

    @Autowired
    private GitLabService gitLabService;

    @Autowired
    private GitLabProjectService projectService;



    public List<String> getBranches(String token, String uri, String groupName, String name) {

        log.debug(" -- [GroupName:{}], [ProjectName:{}] 조회를 시작합니다.", groupName, name);

        String gitProjectId = Optional.ofNullable(projectService.getGitProjectId(token, uri, groupName, name)).orElseThrow(NoSuchElementException::new);
        if ( gitProjectId == null ) {
            log.info("ProjectId가 존재하지 않아, Commit 정보는 수집하지 않습니다.");
            return new ArrayList<>();
        }

        if ( !projectService.isGitProjectAccessable(token, uri, groupName, name) ) {
            log.info("Project가 접근 가능한 상태가 아닙니다. Commit 정보를 수집하지 않습니다.");
            return new ArrayList<>();
        }

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));

        ResponseEntity<List<Map<String, Object>>> responseEntity = null;
        try {
            responseEntity = (ResponseEntity<List<Map<String, Object>>>) gitLabService.callGitLabServer(
                    uri + "/projects/" + gitProjectId + "/repository/branches?per_page=10",
                    HttpMethod.GET,
                    requestEntity,
                    List.class);
        } catch(Exception e) {
            log.error("[GroupName:{}], [ProjectName:{}] Error !!", groupName, name);
            return new ArrayList<>();
        }

        log.debug(" -- [GroupName:{}], [ProjectName:{}] Branch ({})건 조회 되었습니다.", groupName, name, responseEntity.getBody().size());

        // x.x.x 형태로 버전을 준수한 Tag 명칭만 획득
        return
                responseEntity.getBody().stream()
                        .map(entry -> (String) entry.get("name"))
                        .collect(Collectors.toList());
    }



    public List<String> getTags(String token, String uri, String groupName, String name) {

        log.debug(" -- [GroupName:{}], [ProjectName:{}] 조회를 시작합니다.", groupName, name);

        String gitProjectId = Optional.ofNullable(projectService.getGitProjectId(token, uri, groupName, name)).orElseThrow(() -> new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg()));
        if ( gitProjectId == null ) {
            log.info("ProjectId가 존재하지 않아, Commit 정보는 수집하지 않습니다.");
            return new ArrayList<>();
        }

        if ( !projectService.isGitProjectAccessable(token, uri, groupName, name) ) {
            log.info("Project가 접근 가능한 상태가 아닙니다. Commit 정보를 수집하지 않습니다.");
            return new ArrayList<>();
        }

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));

        ResponseEntity<List<Map<String, Object>>> responseEntity = null;
        try {
            responseEntity = (ResponseEntity<List<Map<String, Object>>>) gitLabService.callGitLabServer(
                    uri + "/projects/" + gitProjectId + "/repository/tags?per_page=10",
                    HttpMethod.GET,
                    requestEntity,
                    List.class);
        } catch(Exception e) {
            log.error("[GroupName:{}], [ProjectName:{}] Error !!", groupName, name);
            throw e;
        }

        log.debug(" -- [GroupName:{}], [ProjectName:{}] Tag ({})건 조회 되었습니다.", groupName, name, responseEntity.getBody().size());

        // x.x.x 형태로 버전을 준수한 Tag 명칭만 획득
        return
            responseEntity.getBody().stream()
//                    .filter(entry -> ((String)entry.get("name")).matches("^([0-9]|[1-9][0-9]*)\\.([0-9]|[1-9][0-9]*)\\.([0-9]|[1-9][0-9]*)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+)?$") )
                    .map(entry -> (String) entry.get("name"))
                    .collect(Collectors.toList());

    }
}
