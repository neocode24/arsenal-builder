package com.kt.arsenal.biz.common.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.kt.arsenal.common.constant.ConstUtil.BODY_RETURN_TYPE;
import com.kt.arsenal.common.exception.CustomArsenalException;
import com.kt.arsenal.common.exception.CustomStatusCd;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ClusterApiService {
    
    @Value("${spring.profiles.active}")
    private String springActiveEnv;
    
    /**
     * HTTPRequest 호출
     * osClitenAPI로 안되는 부분에 대한 REST API 호출용. 예) DeploymentConfig/ Deployment 의 Scale변경(osClient로 호출시 EnvName 에 .이 있는경우 오류발생) 
     * 2019.07.08 변경
     * @param token - 로그인용 cluster token
     * @param data - Post로 던질 Json을 String으로 셋팅
     * @param apiUrl - DB에 존재하는 대상 URL을 제외한 뒷부분(API) 조합하여 셋팅(예 /oapi/v1/namespaces/aegistest2/deploymentconfigs/jenkins/scale)
     * @param requestMethod - PUT호출
     */
    public String getHttpsRequest(String token, String data, String clusterUrl, String apiUrl, String requestMethod) {
        
        log.debug("@getHttpsRequest ======>{}" );
        log.debug("springActiveEnv ==============> {}", springActiveEnv);
        
        String responsStatus = null;
        
        try {
            
            TrustManager[] trustAllCerts = {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        // TODO Auto-generated method stub
                    }
                    
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        // TODO Auto-generated method stub
                    }
                    
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        // TODO Auto-generated method stub
                        return null;
                    }
                }
            };
            
	        SSLContext sc = SSLContext.getInstance("SSL");
	        
	        HostnameVerifier hv = new HostnameVerifier() {
	            public boolean verify(String arg0, SSLSession arg1) {
	                // TODO Auto-generated method stub
	                return true;
	            }
	        };
	        
	        sc.init(null, trustAllCerts, new SecureRandom());
	        
	        URL url = new URL(
                    clusterUrl
	                + apiUrl
	                ); // 호출할 url
	        
	        String authorization ="";
	        log.debug("HTTP 호출 : " + url.toString() );
            log.debug("requestMethod : " + requestMethod );

            authorization = "Bearer "+ token;
            log.debug("authorization : " + authorization );
            
            log.debug("data : " + data);
            
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
            HttpsURLConnection connhttps = null;
            HttpURLConnection connhttp = null;
            
            boolean httpsind = "https".equals(url.getProtocol());
            
            if (httpsind) {
                connhttps = (HttpsURLConnection)url.openConnection();
                connhttps.setRequestMethod(requestMethod); // PUT / GET
                connhttps.addRequestProperty("Authorization", authorization);
                
                connhttps.addRequestProperty("Content-Type", "application/json");
                connhttps.addRequestProperty("Accept", "application/json");
                connhttps.setDoOutput(true);
            }
            else {
                connhttp = (HttpURLConnection)url.openConnection();
                connhttp.setRequestMethod(requestMethod); // PUT / GET
                connhttp.addRequestProperty("Authorization", authorization);
                
                connhttp.addRequestProperty("Content-Type", "application/json");
                connhttp.addRequestProperty("Accept", "application/json");
                connhttp.setDoOutput(true);
            }
            
            if (("POST".equals(requestMethod) || "PUT".equals(requestMethod)) && data != null) {
                OutputStreamWriter writer = new OutputStreamWriter(httpsind ? connhttps.getOutputStream() : connhttp.getOutputStream());
                writer.write(data);
                writer.flush();
                writer.close();
            }
            
            String line;
            StringBuffer  buffer = new StringBuffer();
            
            int statusCode = httpsind ? connhttps.getResponseCode() : connhttp.getResponseCode();
            
            InputStream is = null;
            
            if (statusCode >= 200 && statusCode < 400) {
                is = httpsind ? connhttps.getInputStream() : connhttp.getInputStream();
            } else {
                is = httpsind ? connhttps.getErrorStream() : connhttp.getErrorStream();
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(is)) ;
            
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            
            reader.close();
            
            responsStatus = String.valueOf(httpsind ? connhttps.getResponseCode() : connhttp.getResponseCode());
            log.debug("responsStatus ==> " + responsStatus);
            
            if(httpsind)
                connhttps.disconnect();
            else
                connhttp.disconnect();
            
            if (!(statusCode >= 200 && statusCode < 400)) {
                throw new CustomArsenalException(buffer.toString());
            }
            
            return buffer.toString();
        
        } catch (Exception e) {
            log.info("getLocalizedMessage : " + e.getLocalizedMessage());
            log.info("getMessage : " + e.getMessage());
            log.info("getCause : " + e.getCause());
            log.info("ex.toString : " + e.toString());            
            throw new CustomArsenalException(CustomStatusCd.CD_9999.getCd(), e.getMessage(), e);
        }
    }
    
    

    /**
     * callClusterApiService
     * 8210747 (2019.07.31) 
     * @return Map
     * @throws IOException 
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> callClusterApiService(String token, String url, String apiUri, String requestMethod, int returyType , String requestBody) throws JsonParseException, JsonMappingException, IOException {
        log.debug("call callClusterApiService ==> {}", apiUri);
        JSONObject resultJsonObj = null;

        // HTTPRequest 호출
        String result = getHttpsRequest(token,
                (requestBody == null || "".equals(requestBody)) ? null : requestBody,
                url,
                apiUri,
                requestMethod);
        
        Map<String, Object> resultMap = null;
        
        if ((resultJsonObj = (JSONObject) JSONValue.parse(result)) != null) {
            if (returyType == BODY_RETURN_TYPE.JSON) {
                resultMap = new HashMap<String, Object>(); 
                resultMap.put("jsonObject", resultJsonObj);
                resultMap.put("jsonString", result);
                JsonNode jsonNodeTree = new ObjectMapper().readTree(result);
                resultMap.put("yamlString", new YAMLMapper().writeValueAsString(jsonNodeTree));
            }
            else if (returyType == BODY_RETURN_TYPE.MAP) {
                resultMap = new ObjectMapper().readValue(resultJsonObj.toJSONString(), Map.class);
            }
            
        }

        return resultMap;
    }
    
    /**
     * callClusterApiService
     * 8210747 (2019.07.03) 
     * @return Map
     * @throws IOException 
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    public Map<String, Object> callClusterApiService(String token, String url, String apiUri, String requestMethod, String requestBody) throws JsonParseException, JsonMappingException, IOException {
        return callClusterApiService(token, url, apiUri, requestMethod, BODY_RETURN_TYPE.MAP, requestBody);
    }
}
