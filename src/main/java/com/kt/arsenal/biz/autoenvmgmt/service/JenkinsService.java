package com.kt.arsenal.biz.autoenvmgmt.service;

import com.kt.arsenal.common.exception.CustomArsenalException;
import com.kt.arsenal.common.exception.CustomStatusCd;
import com.kt.arsenal.common.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Arsenal Dev JenkinsService
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2019-07-08
 */

@Slf4j
@Service
public class JenkinsService {

    RestTemplate restTemplate;

    @Autowired
    private CommonUtil commonUtil;

    @Autowired
    public JenkinsService(RestTemplateBuilder restTemplateBuilder) {

        restTemplate = restTemplateBuilder
                .errorHandler(new JenkinsErrorHandler())
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();


        // Jenkins Https 사설 인증서에 대한 Varification 무시
        restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory() {

            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
                    SSLContext sc;
                    try {
                        sc = SSLContext.getInstance("SSL");
                        sc.init(null, new TrustManager[] { new X509TrustManager() {

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                            }

                            @Override
                            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                            }
                        }}, new SecureRandom());
                        ((HttpsURLConnection) connection).setSSLSocketFactory(sc.getSocketFactory());
                    } catch (NoSuchAlgorithmException e) {
                        log.error("error", e);
                    } catch (KeyManagementException e) {
                        log.error("error", e);
                    }
                }
                super.prepareConnection(connection, httpMethod);
            }
        });



    }

    /**
     * Jenkins Server 호출 (GET/POST)
     * @param uri Jenkins URI
     * @param httpMethod Jenkins HttpMethod(GET, POST, PUT, DELETE)
     * @param httpEntity Jenkins HttpEntity
     * @param responseClass Response Class Type
     * @return ResponseEntity
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ResponseEntity<?> callJenkinsServer(String uri, HttpMethod httpMethod, HttpEntity httpEntity, Class responseClass) {
        ResponseEntity<?> responseEntity = restTemplate.exchange(
                uri,
                httpMethod,
                httpEntity,
                responseClass
        );
        return validateResponseEntity(responseEntity, httpMethod);
    }


    /**
     * Jenkins Server 호출.
     * 파일 바이너리를 전송해야 함에 있어서, RestTemplate로는 Binary Stream 전송에 명확하지 않음.
     * 따라서, 직접적인 URLConnection 을 사용하여 처리 함.
     *
     * @param token Jenkins Token
     * @param itemName Jenkins Item(pipeline) Name
     */
    public void callJenkinsServerWithBinary(String token, String id, String uri, String itemName, byte[] configBinary) {

//        HttpsURLConnection openConnection = getHttpsURLConnection(uri);
        HttpURLConnection openConnection = getHttpURLConnection(token, id, uri);


        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(openConnection.getOutputStream()));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(configBinary)));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
            bufferedReader.close();
            bufferedWriter.flush();
            bufferedWriter.close();

            openConnection.connect();

            if (openConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {

                bufferedReader = new BufferedReader(new InputStreamReader(openConnection.getErrorStream()));
                do {
                    log.error(bufferedReader.readLine());
                } while (bufferedReader.ready());

                log.error("Jenkins 서버로 명령을 실행했지만, 오류가 응답되었습니다. message:[{}]", openConnection.getResponseMessage());
                throw new CustomArsenalException(CustomStatusCd.CD_72001.getCd(), CustomStatusCd.CD_72001.getMsg());
            } else {
                StringBuilder readBuffer = new StringBuilder();
                bufferedReader = new BufferedReader(new InputStreamReader(openConnection.getInputStream()));
                do {
                    readBuffer.append(bufferedReader.readLine());
                } while (bufferedReader.ready());

                log.debug("Jenkins 서버로 명령 실행에 정상 처리 되었습니다. itemName:[{}] message:[{}]", itemName, readBuffer);
            }
        } catch (IOException e) {
            log.error("Jenkins CI/CD 구성파일 생성중에 오류가 발생하였습니다.", e);
            throw new CustomArsenalException(CustomStatusCd.CD_72001.getCd(), CustomStatusCd.CD_72001.getMsg());
        }
    }

    /**
     * Http URL connection 제공 로직. Binary 전송이 Resttemplate 로 명확하지 않아서
     * 직접구현함.
     * @param uri Jenkins URI
     * @return HttpURLConnection
     */
    private HttpURLConnection getHttpURLConnection(String token, String id, String uri) {
        URL url = null;
        HttpURLConnection openConnection = null;

        try {
            url = new URL(uri);
            openConnection = (HttpURLConnection) url.openConnection();

            openConnection.setRequestMethod("POST");
            openConnection.setRequestProperty("Content-Type", "text/xml");


            openConnection.setRequestProperty("Authorization", getAuthorization(token, id));

            openConnection.setDoOutput(true);
            openConnection.setDoInput(true);
            openConnection.setUseCaches(false);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return openConnection;
    }


    /**
     * Https URL Connection 제공 로직. Binary 전송이 Resttemplate 로 명확하지 않아서
     * 직접구현함.
     * @param uri Jenkins URI
     * @return HttpsURLConnection
     */
    private HttpsURLConnection getHttpsURLConnection(String token, String id, String uri) {

        URL url = null;
        HttpsURLConnection openConnection = null;

        try {
            url = new URL(uri);
            openConnection = (HttpsURLConnection) url.openConnection();

            openConnection.setRequestMethod("POST");
            openConnection.setRequestProperty("Content-Type", "text/xml");
            openConnection.setHostnameVerifier((hostname, session) -> true);

            SSLContext sc;
            sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{new X509TrustManager() {

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                }
            }}, new SecureRandom());
            openConnection.setSSLSocketFactory(sc.getSocketFactory());


            openConnection.setRequestProperty("Authorization", getAuthorization(token, id));

            openConnection.setDoOutput(true);
            openConnection.setDoInput(true);
            openConnection.setUseCaches(false);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            log.error("error", e);
        } catch (KeyManagementException e) {
            log.error("error", e);
        }

        return openConnection;
    }


    /**
     * Jenkins REST API 호출 이후, 응답 값 확인 처리 과정.
     * CLIENT_ERROR(4xx)에 대한 에러를  직접 처리 하는 것으로 위임 받았기에, 아래 와 같은 내용들의 에러코드를 항목별로 정의하여 처리해야 함.
     * @param responseEntity ResponseEntity
     * @return ResponseEntity
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
                    log.error("Jenkins 호출 에러 발생하였습니다. {}", message);
                    throw new CustomArsenalException(CustomStatusCd.CD_72002.getCd(), CustomStatusCd.CD_72002.getMsg());
                }
                else {
                    log.error("Jenkins 호출 후 알수 없는 오류가 발생되었습니다. {}", message);
                    throw new CustomArsenalException(CustomStatusCd.CD_72001.getCd(), CustomStatusCd.CD_72001.getMsg());
                }

                // TODO : TEST 진행하면서, 오류 항목 추가 정의 필요.
            }
            // 404 오류는 Jenkins에서 요청사항에 대한 결과가 없음으로 별도 에러처리 하지 않음.
            else if ( responseEntity.getStatusCode() == HttpStatus.NOT_FOUND ) {
                String message = String.valueOf(responseEntity.getBody());
                log.info("Jenkins REST API 요청결과가 없습니다 [message:{}]", message);
            }
            // 400 이외 에는 일괄 오류 재전달
            else {
                throw new HttpClientErrorException(responseEntity.getStatusCode(), responseEntity.toString());
            }
        }

        return responseEntity;
    }

    /**
     * Jenkins 호출 Authorization
     * @param token Jenkins Token
     * @return Basic Authentication Token String
     */
    private String getAuthorization(String token, String id) {
        String auth = id + ":" + token;
        return "Basic " + new String(Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII"))));
    }

    /**
     * Jenkins 호출하기 위한 Header. (Token 추가 로직)
     * @param token Jenkins Token
     * @return HttpHeaders
     */
    public HttpHeaders createHeader(String token, String id) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getAuthorization(token, id));

        return headers;
    }

    /**
     * Jenkins 호출 시 Form 전송하기 위한 Header
     * @param token Jenkins Token
     * @return HttpHeaders
     */
    public HttpHeaders createFormHeader(String token, String id) {
        final HttpHeaders headers = createHeader(token, id);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        return headers;
    }

    /**
     * Jenkins 호출 시 Binary를 전송하기 위한 Header
     * @param token Jenkins Token
     * @return HttpHeaders
     */
    public HttpHeaders createMultipartHeader(String token, String id) {
        final HttpHeaders headers = createHeader(token, id);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return headers;
    }
}
