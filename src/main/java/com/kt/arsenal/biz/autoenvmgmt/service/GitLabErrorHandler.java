package com.kt.arsenal.biz.autoenvmgmt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

/**
 * Arsenal Dev GitLab Error Handler
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2019-06-24
 */
@Slf4j
@Component
public class GitLabErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return ( response.getStatusCode().series() == HttpStatus.Series.CLIENT_ERROR || response.getStatusCode().series() == HttpStatus.Series.SERVER_ERROR );
    }

    /**
     * GitLab 호출 시, CLIENT ERROR(4xx) 에 대해서는 직접 처리 하도록 함.
     * 4xx 에러는 일괄 위임.
     *
     * @param response Response
     * @throws IOException IOException
     */
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        if ( response.getStatusCode().series() == HttpStatus.Series.SERVER_ERROR ) {
            log.info("GitLab Http 5xx 에러 발생. {]", response.toString());
            throw new HttpServerErrorException(response.getStatusCode(), response.getStatusText());
        }
        else if ( response.getStatusCode().series() == HttpStatus.Series.CLIENT_ERROR ) {
            log.debug("GitLab Http 4xx 에러 발생. {}, {}", response.getStatusText(), response.toString());
        }
        else {
            log.info("Unkown Error Series");
            throw new HttpServerErrorException(response.getStatusCode(), response.getStatusText());
        }
    }
}
