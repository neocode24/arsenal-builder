package com.kt.arsenal.biz.autoenvmgmt.service;

import com.kt.arsenal.common.exception.CustomArsenalException;
import com.kt.arsenal.common.exception.CustomStatusCd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Arsenal Dev Item Service
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2019-07-08
 */

@Slf4j
@Service
public class JenkinsItemService {

    @Autowired
    private JenkinsService jenkinsService;



    /**
     * Jenkins Pipeline(Item) 삭제
     * @param token Jenkins Token
     * @param uri Jenkins Uri
     * @param id Jenkins Id
     * @param itemName Jenkins Item 명칭
     */
    @SuppressWarnings({ "unchecked", "unused" })
    public void removeItem(String token, String uri, String id, String itemName) {
        log.debug("Jenkins removeItem token:{}, uri:{}, id:{}, itemName:{}", token, uri, id, itemName);
        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(token, id));

        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) jenkinsService.callJenkinsServer(
                uri + "/job/" + itemName +"/doDelete",
                HttpMethod.POST,
                requestEntity,
                Map.class
        );
    }

    /**
     * Jenkins Item(Pipeline) 조회
     * @param token Jenkins Item
     * @param uri Jenkins Uri
     * @param id Jenkins Id
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
    
    /**
     * Jenkins Item(Pipeline) configuration 조회
     * @param token Jenkins Item
     * @param uri Jenkins Uri
     * @param id Jenkins Id
     * @param itemName Jenkins Item 명칭
     * @return Jenkins XML
     */
    @SuppressWarnings("unchecked")
    public String findItemConfig(String token, String uri, String id, String itemName) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(token, id));

        ResponseEntity<String> responseEntity = (ResponseEntity<String>) jenkinsService.callJenkinsServer(
                uri + "/job/" + itemName +"/config.xml",
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        return responseEntity.getBody();
        
    }



    /**
     * Jenkins Item(Pipeline) 조회
     * @param token Jenkins Item
     * @param uri Jenkins Uri
     * @param id Jenkins Id
     * @param matchName 특정 명칭을 대표하는 name
     * @return "_class", "name", "url", "color" 4가지 key로 List로 제공함.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findItemsInMatchString(String token, String uri, String id, String matchName) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(jenkinsService.createHeader(token, id));

        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) jenkinsService.callJenkinsServer(
                uri + "/view/All/api/json",
                HttpMethod.GET,
                requestEntity,
                Map.class
        );

        String matchString = matchName.toUpperCase() + "-";

        // 전체 pipeline 정보 획득
        List<Map<String, Object>> allItems = (List<Map<String, Object>>) Optional.ofNullable(responseEntity.getBody()).orElseGet(HashMap::new).get("jobs");

        // 특정 이름으로 시작되는 pipeline 명칭만 획득. (map 구조이며, 내부에는 "name", "url" 로 되어 있음)
        List<Map<String, Object>> filteredList = allItems.stream().filter(f -> f.get("name").toString().indexOf(matchString) > -1).collect(Collectors.toList());

        log.debug("[{}] filted jenkins items : {}", matchString, filteredList.toString());

        return filteredList;
    }
    
    /**
     * Jenkins Item(pipeline) 변경
     *
     * @param token Jenkins Token
     * @param uri Jenkins Uri
     * @param id Jenkins Id
     * @param itemName Jenkins pipline name (Cluster Namespace)
     * @param value config
     * @return itemName
     */
    public String updateItem(String token, String uri, String id, String itemName, byte[] value) {
        
        log.debug("Jenkins ItemName :[{}] update item(pipeline) !! ", itemName);

        if ( findItem(token, uri, id, itemName) != null ) {
            log.error("itemName:[{}]은 이미 존재합니다. 변경 진행하지 않습니다.", itemName);
            throw new CustomArsenalException(CustomStatusCd.CD_72001.getCd(), CustomStatusCd.CD_72001.getMsg());
        }
        
        
        jenkinsService.callJenkinsServerWithBinary(
                token,
                (uri + "/job/" + itemName + "/config.xml"),
                id,
                itemName,
                value
        );

        log.info("JenkinsItemName:[{}] 변경이 완료되었습니다.", itemName);
        return itemName;
        
    }
    
    /**
     * Jenkins Item(pipeline) 변경
     *
     * @param token Jenkins Token
     * @param uri Jenkins Uri
     * @param id Jenkins Id
     * @param itemName Jenkins pipline name (Cluster Namespace)
     * @param value config
     * @return itemName
     */
    public String updateItem(String token, String uri, String id, String itemName, String value) {
        
        final HttpEntity<?> requestEntity = new HttpEntity<>(value, jenkinsService.createFormHeader(token, id));

        @SuppressWarnings("unchecked")
        ResponseEntity<String> responseEntity = (ResponseEntity<String>) jenkinsService.callJenkinsServer(
                (uri + "/job/" + itemName + "/config.xml"),
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if ( responseEntity.getStatusCode() == HttpStatus.OK ) {
            log.debug("Jenkins Pipeline[{}]이 변경되었습니다.", itemName);
            return itemName;
        } else {
            return null;
        }
        
    }
    
}
