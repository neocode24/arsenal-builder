package com.kt.arsenal.biz.autoenvmgmt.service;

import com.kt.arsenal.common.exception.CustomArsenalException;
import com.kt.arsenal.common.exception.CustomStatusCd;
import com.kt.arsenal.common.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

/**
 * Arsenal Dev GitLabService
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2019-06-25
 */

@Slf4j
@Service
public class GitLabService {

    RestTemplate restTemplate;

    @Autowired
    private CommonUtil commonUtil;



    @Autowired
    public GitLabService(RestTemplateBuilder restTemplateBuilder) {

        restTemplate = restTemplateBuilder
                .errorHandler(new GitLabErrorHandler())
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
    }


    /**
     * GiLab Server 호출 (GET)
     * @param uri GitLab URI
     * @param httpMethod HTTP Request Method
     * @param httpEntity HTTP Request Entity
     * @param param Request Map
     * @return Response Entity
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ResponseEntity<?> callGitLabServer(String uri, HttpMethod httpMethod, HttpEntity httpEntity, Class responseClass, Map<String, Object> param) {
        try {
            ResponseEntity<?> responseEntity = restTemplate.exchange(
                    uri,
                    httpMethod,
                    httpEntity,
                    responseClass,
                    param
            );
            return validateResponseEntity(responseEntity, httpMethod);
        }
        catch ( Exception e ) {
            throw new CustomArsenalException(CustomStatusCd.CD_72000.getCd(), CustomStatusCd.CD_72000.getMsg(), e);
        }
        
    }

    /**
     * GiLab Server 호출 (GET/POST)
     * @param uri GitLab URI
     * @param httpMethod HTTP Request Method
     * @param httpEntity Http Request Entity
     * @return Response Entity
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ResponseEntity<?> callGitLabServer(String uri, HttpMethod httpMethod, HttpEntity httpEntity, Class responseClass){
        ResponseEntity<?> responseEntity = null;
        try {
        	
        	responseEntity = restTemplate.exchange(
        		new URI(uri),
                httpMethod,
                httpEntity,
                responseClass
        	);
        	
        } catch (URISyntaxException e) {
            log.error("URL Encoding 중에 오류가 발생하였습니다.", e);
            throw new CustomArsenalException(CustomStatusCd.CD_72001.getCd(), CustomStatusCd.CD_72001.getMsg());
        }
        return validateResponseEntity(responseEntity, httpMethod);
    }

    /**
     * GitLab Server 호출 (GET/POST)
     * URI에 encoding이 들어간 경우 필히 위 메소드를 사용하여야 함.
     * restTemplate에서 URI을 String 또는 URI가 가능한데, Encoding이 된 경우 URI 객체로 전달해야 함.
     *
     * @param uri GitLab URI
     * @param httpMethod HTTP Request Method
     * @param httpEntity Http Request Entity
     * @return Response Entity
     */
    @SuppressWarnings("rawtypes")
    public ResponseEntity<?> callGitLabServer(String uri, HttpMethod httpMethod, HttpEntity httpEntity) {
        log.debug("uri:{}", uri);
        ResponseEntity<?> responseEntity = null;
        try {
        	responseEntity = restTemplate.exchange(
			        new URI(uri),
			        httpMethod,
			        httpEntity,
			        Map.class
			);
        } catch (URISyntaxException e) {
            log.error("URL Encoding 중에 오류가 발생하였습니다.", e);
            throw new CustomArsenalException(CustomStatusCd.CD_72001.getCd(), CustomStatusCd.CD_72001.getMsg());
        }
        
        return validateResponseEntity(responseEntity, httpMethod);
        
    }


    /**
     * GitLab REST API 호출 이후, 응답 값 확인 처리 과정.
     * CLIENT_ERROR(4xx)에 대한 에러를  직접 처리 하는 것으로 위임 받았기에, 아래 와 같은 내용들의 에러코드를 항목별로 정의하여 처리해야 함.
     *
     *
     * GitLab Https Response Code Set
     *  200 OK : GET, PUT or DELETE request was successful.
     *  204 No Content : The Server has successfully fulfiled the request and that there is no additional content to send in the response payload body.
     *  201 Created : Post request was successful and the resource is returned as JSON.
     *  304 Not Modified : Indicates that the resource has not been modified since the last request.
     *  400 Bas Request : A required attribute of the API request is missing.
     *  401 Unauthorized
     *  403 Forbidden
     *  404 Not found
     *  405 Method Not allowed
     *  409 Conflict
     *  412
     *  422 Unprocessable
     *  500 Server Error
     * @param responseEntity Response Entity
     * @return
     */
    public ResponseEntity<?> validateResponseEntity(ResponseEntity<?> responseEntity, HttpMethod httpMethod) {
//        log.debug("responseCode:{}, responseBody:{}",
//                responseEntity.getStatusCode(), commonUtil.getPrettyJsonFormat(responseEntity.getBody()));


        // 4xx 에러 발생에 대한 처리
        if ( responseEntity.getStatusCode().series() == HttpStatus.Series.CLIENT_ERROR ) {

            // POST로 전달한 400 에러 인 경우
            if ( responseEntity.getStatusCode() == HttpStatus.BAD_REQUEST && httpMethod == HttpMethod.POST ) {
                String message = String.valueOf(responseEntity.getBody());

                // 이미 생성된 Group이 있는 경우
                if ( message.indexOf("has already been taken") > -1 ) {
                    log.error("GitLab 호출 에러 발생하였습니다. {}", message);
                    throw new CustomArsenalException(CustomStatusCd.CD_72002.getCd(), CustomStatusCd.CD_72002.getMsg());
                }
                else {
                    log.error("GitLab 호출 후 알수 없는 오류가 발생되었습니다. {}", message);
                    throw new CustomArsenalException(CustomStatusCd.CD_72001.getCd(), CustomStatusCd.CD_72001.getMsg());
                }

                // TODO : TEST 진행하면서, 오류 항목 추가 정의 필요.
            }
            // 조회가 없는 경우에 대한 오류는 무시함.
            else if ( responseEntity.getStatusCode()== HttpStatus.NOT_FOUND ) {
                log.debug("GitLab REST API 404 발생. 404는 무시합니다.");
            }
            // 400 이외 에는 일괄 오류 재전달
            else {
                throw new HttpClientErrorException(responseEntity.getStatusCode(), responseEntity.toString());
            }
        }

        return responseEntity;
    }

    /**
     * GitLab 호출하기 위한 Header. (Token 추가 로직)
     * @param token GitLab Token
     * @return HttpHeaders
     */
    public HttpHeaders createHeader(String token) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Private-Token", token);

        return headers;
    }

    /**
     * GitLab 호출 후 Binary를 수신하기 위한 Header
     * @param token GitLab Token
     * @return HttpHeaders
     */
    public HttpHeaders createBinaryHeader(String token) {
        final HttpHeaders headers = createHeader(token);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));

        return headers;
    }

    /**
     * GitLab 호출 시 Binary를 전송하기 위한 Header
     * @param token GitLab Token
     * @return HttpHeaders
     */
    public HttpHeaders createMultipartHeader(String token) {
        final HttpHeaders headers = createHeader(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return headers;
    }
}
