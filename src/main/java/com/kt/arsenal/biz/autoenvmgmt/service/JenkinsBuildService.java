package com.kt.arsenal.biz.autoenvmgmt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Arsenal Dev Build Service
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2019-11-22
 */

@Slf4j
@Service
public class JenkinsBuildService {

    @Autowired
    private JenkinsService jenkinsService;


    public Map<String, Object> lastBuildStatus(String token, String uri, String id, String itemName) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(token, id));

        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) jenkinsService.callJenkinsServer(
                uri + "/job/" + itemName +"/lastBuild/api/json",
                HttpMethod.GET,
                requestEntity,
                Map.class
        );

        // 최근 build 정보 획득
        return Optional.ofNullable(responseEntity.getBody()).orElseGet(HashMap::new);
    }

    /**
     * Jenkins 최근 5개의 Build Status를 제공함.
     * @param token
     * @param uri
     * @param id
     * @param itemName
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> buildStatus(String token, String uri, String id, String itemName) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(token, id));

        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) jenkinsService.callJenkinsServer(
                uri + "/job/" + itemName +"/api/json?tree=builds[number,id,timestamp,result,duration]",
                HttpMethod.GET,
                requestEntity,
                Map.class
        );

        // build item의 최근 5개의 status 정보 획득
        List<Map<String, Object>> buidStatusList = (List<Map<String, Object>>) Optional.ofNullable(responseEntity.getBody()).orElseGet(HashMap::new).get("builds");

        // timestamp 값이 long type으로 되어 있으나, local 정보로 변경하여, 처리 함.
        for ( Map<String, Object> status : buidStatusList ) {
            LocalDateTime localTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli((Long) status.get("timestamp")), TimeZone.getDefault().toZoneId());
            status.put("timestamp", localTimeStamp);
        }

        log.debug("[{}] jenkins build status : {}", itemName, buidStatusList.toString());

        return buidStatusList;
    }



    /**
     * "-DEV-DEPLOY" 로 종료되는 item 일 경우 기본 build로 실행
     * 함.
     * @param token
     * @param uri
     * @param itemName
     * @return
     */
    public boolean buildDevDeployItem(String token, String uri, String id, String itemName) {
        return buildItem(token, uri, id, itemName);
    }

    /**
     * "-DOCKERIZE" 로 종료되는 item 일 경우 기본 parameter(master, false)을 설정하여, withParameter로 실행 함.
     * @param token
     * @param uri
     * @param itemName
     * @param freeshStart
     * @return true:성공, false:실패
     */
    public boolean buildDockerizeItem(String token, String uri, String id, String itemName, boolean freeshStart) {
        return buildItemWithParameters(token, uri, id, itemName, "branchName=master", "freshStart=" + String.valueOf(freeshStart));
    }

    /**
     * "-TAG" 로 종료되는 item 일 경우 parameter을 설정하여, withParameter로 실행 함.
     * @param token
     * @param uri
     * @param itemName
     * @param branchName
     * @param freshStart
     * @return
     */
    public boolean buildTagItem(String token, String uri, String id, String itemName, String branchName, boolean freshStart) {
        String[] parameters = new String[2];
        parameters[0] = "branchName=" + branchName;
        parameters[1] = "freshStart=" + String.valueOf(freshStart);

        return buildItemWithParameters(token, uri, id, itemName, parameters);
    }

    /**
     * Jenkins Item Build
     * @param token jenkins token
     * @param uri jenkins uri
     * @param id Jenkins Id
     * @param itemName jenkins item name
     * @return true:성공, false:실패
     */
    @SuppressWarnings("unchecked")
    public boolean buildItem(String token, String uri, String id, String itemName) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(token, id));

        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) jenkinsService.callJenkinsServer(
                uri + "/job/" + itemName + "/build",
                HttpMethod.POST,
                requestEntity,
                Map.class
        );

        if ( responseEntity.getStatusCode() == HttpStatus.CREATED ) {
            log.debug("Jenkins Item:[{}]이 실행되었습니다.", itemName);
            return true;
        } else {
            return false;
        }
    }


    /**
     * Jenkins Build with parameters
     * @param token Jenkins token
     * @param uri Jenkins uri
     * @param id Jenkins id
     * @param itemName Jenkins item name
     * @param parameters paramters
     * @return true:성공, false:실패
     */
    @SuppressWarnings("unchecked")
    public boolean buildItemWithParameters(String token, String uri, String id, String itemName, String ...parameters) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(token, id));

        String param = String.join("&", parameters);

        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) jenkinsService.callJenkinsServer(
                uri + "/job/" + itemName + "/buildWithParameters?" + param,
                HttpMethod.POST,
                requestEntity,
                Map.class
        );

        if ( responseEntity.getStatusCode() == HttpStatus.CREATED ) {
            log.info("Jenkins Item:[{}]이 Parameter:[{}] 과 같이 실행되었습니다.", itemName, param);
            return true;
        } else {
            return false;
        }
    }

    public boolean stopBuildItem(String token, String uri, String id, String itemName, int buildNumber) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(token, id));

        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) jenkinsService.callJenkinsServer(
                uri + "/job/" + itemName + "/" + buildNumber + "/stop",
                HttpMethod.POST,
                requestEntity,
                Map.class
        );

        if ( responseEntity.getStatusCode() == HttpStatus.FOUND ) {
            log.info("Jenkins Item:[{}]이 중단되었습니다.", itemName);
            return true;
        } else {
            return false;
        }
    }


    /**
     * Jenkins Item(Pipeline) 조회
     * @param token Jenkins Item
     * @param uri Jenkins Uri
     * @param id Jenkins id
     * @param itemName Jenkins Item 명칭
     * @return Jenkins Item Key/Value Map
     */
    @SuppressWarnings("unchecked")
    public Map<?, ?> findItem(String token, String uri, String id, String itemName) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(token, id));

        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) jenkinsService.callJenkinsServer(
                uri + "/job/" + itemName +"/api/json",
                HttpMethod.GET,
                requestEntity,
                Map.class
        );

        String findName = String.valueOf(Optional.ofNullable(responseEntity.getBody()).orElseGet(HashMap::new).get("name"));
        if ( itemName.equals(findName) ) {
            return responseEntity.getBody();
        }
        return null;
    }



}
