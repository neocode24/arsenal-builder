package com.kt.arsenal.biz.builder.controller;

import com.kt.arsenal.biz.autoenvmgmt.service.JenkinsViewService;
import com.kt.arsenal.biz.builder.domain.BuilderInfo;
import com.kt.arsenal.biz.builder.domain.BuilderJobRequestDomain;
import com.kt.arsenal.biz.builder.domain.BuilderRequestDomain;
import com.kt.arsenal.biz.builder.domain.LastBuildInfo;
import com.kt.arsenal.biz.builder.service.BuilderService;
import com.kt.arsenal.biz.portal.domain.BusinessTopologyDomain;
import com.kt.arsenal.biz.portal.service.PortalCallerService;
import com.kt.arsenal.common.exception.CustomArsenalException;
import com.kt.arsenal.common.exception.CustomStatusCd;
import com.kt.arsenal.common.model.RestMessage;
import com.kt.arsenal.common.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;

import static com.kt.arsenal.biz.autoenvmgmt.service.JenkinsViewService.ARSENAL_EXECUTOR_FIELD_ID;

/**
 * Arsenal-Dev Builder Controller
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/09/02
 */

@Slf4j
@RestController
@RequestMapping(value = "/builder")
public class BuilderController {


    @Autowired
    private JenkinsViewService jenkinsViewService;

    @Autowired
    private PortalCallerService portalCallerService;

    @Autowired
    private BuilderService builderService;


    private List<BusinessTopologyDomain> businessTopologyDomains;


    /**
     * BusinessTopology 정보를 Portal로 부터 초기 구성함.
     */
    @PostConstruct
    public void postConstruct() {

        log.info(" -- start PostConstruct !!");
        initBusinessToplogy();
        log.info(" -- end PostConstruct !!");
    }


    @PostMapping("/stop")
    public ResponseEntity<?> stopBuild(@RequestBody final BuilderJobRequestDomain requestDomain, final RestMessage restMessage, Authentication authentication) {

        // NameSpace User 체크
        if (!CommonUtil.isNamespaceUser(authentication, 1L, requestDomain.getServiceAliasName())) {
            throw new CustomArsenalException(CustomStatusCd.CD_403.getCd(), CustomStatusCd.CD_403.getMsg());
        }

        // 사업 별칭과 Jenkins view 명칭이 다른경우
        if (!requestDomain.getServiceAliasName().equals(requestDomain.getJobInfo().getViewName())) {
            log.warn("서비스 별칭과 Job의 View이름이 일치하지 않습니다. [requestDomain:{}]", requestDomain);

            restMessage.setNG();
            restMessage.setMessage("입력 데이터가 올바르지 않습니다.");
            return ResponseEntity.ok(restMessage);
        }

        // 필수 값 체크
        if ( StringUtils.isEmpty(requestDomain.getJobInfo().getName())
                || StringUtils.isEmpty(requestDomain.getJobInfo().getUrl())
                || StringUtils.isEmpty(requestDomain.getJobInfo().getViewName())
                || requestDomain.getJobInfo().getParameterDefinitionMap() == null) {
            log.warn("빌드 수행을 위한 기본값이 전달되지 않았습니다. [jobName:{}], [jobUrl:{}], [viewName:{}], [parameterMap:{}]",
                    requestDomain.getJobInfo().getUrl(),
                    requestDomain.getJobInfo().getUrl(),
                    requestDomain.getJobInfo().getViewName(),
                    requestDomain.getJobInfo().getParameterDefinitionMap());

            restMessage.setNG();
            restMessage.setMessage("빌드 수행을 위한 기본값이 전달되지 않았습니다.");
            return ResponseEntity.ok(restMessage);
        }

        try {
            initBusinessToplogy();

            log.info("-- Arsenal Portal 로 부터 Jenkins DevTools 정보를 일괄로 획득합니다.");
            portalCallerService.setDevToolsInfo(requestDomain, businessTopologyDomains);

            // 요청 Domain 에서 Job정보로 Build를 종료함.
            builderService.stopBuild(requestDomain);

        } catch (CustomArsenalException e) {
            restMessage.setNG();
            restMessage.setMessage(e.getMessage());
            return ResponseEntity.ok(restMessage);

        } catch (Exception e) {
            log.error("{}", e);

            restMessage.setNG();
            restMessage.setMessage(e.getMessage());
            return ResponseEntity.ok(restMessage);
        }

        restMessage.setOK();
        return ResponseEntity.ok(restMessage);

    }


    /**
     * Jenkins Job을 수행 함.
     * Parameter로 JobInfo를 전달 받으나, JobInfo 안에 있는 BuildHistory는 전달되지 않아도 됨.
     * @param requestDomain
     * @param restMessage
     * @param authentication
     * @return
     */
    @PostMapping("/build")
    public ResponseEntity<?> doBuild(@RequestBody final BuilderJobRequestDomain requestDomain, final RestMessage restMessage, Authentication authentication) {


        // NameSpace User 체크
        if (!CommonUtil.isNamespaceUser(authentication, 1L, requestDomain.getServiceAliasName())) {
            throw new CustomArsenalException(CustomStatusCd.CD_403.getCd(), CustomStatusCd.CD_403.getMsg());
        }

        // 사업 별칭과 Jenkins view 명칭이 다른경우
        if (!requestDomain.getServiceAliasName().equals(requestDomain.getJobInfo().getViewName())) {
            log.warn("서비스 별칭과 Job의 View이름이 일치하지 않습니다. [requestDomain:{}]", requestDomain);

            restMessage.setNG();
            restMessage.setMessage("입력 데이터가 올바르지 않습니다.");
            return ResponseEntity.ok(restMessage);
        }

        // 필수 값 체크
        if ( StringUtils.isEmpty(requestDomain.getJobInfo().getName())
                || StringUtils.isEmpty(requestDomain.getJobInfo().getUrl())
                || StringUtils.isEmpty(requestDomain.getJobInfo().getViewName())
                || requestDomain.getJobInfo().getParameterDefinitionMap() == null) {
            log.warn("빌드 수행을 위한 기본값이 전달되지 않았습니다. [jobName:{}], [jobUrl:{}], [viewName:{}], [parameterMap:{}]",
                        requestDomain.getJobInfo().getUrl(),
                    requestDomain.getJobInfo().getUrl(),
                    requestDomain.getJobInfo().getViewName(),
                    requestDomain.getJobInfo().getParameterDefinitionMap());

            restMessage.setNG();
            restMessage.setMessage("빌드 수행을 위한 기본값이 전달되지 않았습니다.");
            return ResponseEntity.ok(restMessage);
        }

        // Parameter 중 사용자 정보 없음 오류 발생.
        if ( requestDomain.getJobInfo().getParameterDefinitionMap().entrySet()
                .stream()
                .filter(entry -> entry.getKey().equals(ARSENAL_EXECUTOR_FIELD_ID) && StringUtils.isEmpty((String) entry.getValue()))
                .count() > 0
        ) {
            log.warn("빌드 수행자에 대한 정보가 존재하지 않습니다.", requestDomain.getJobInfo().getParameterDefinitionMap());
            restMessage.setNG();
            restMessage.setMessage("빌드 수행자에 대한 정보가 존재하지 않습니다.");
            return ResponseEntity.ok(restMessage);
        }

       // Parameter 선택되지 않은 채 List로 그대로 입력된 경우
        if ( requestDomain.getJobInfo().getParameterDefinitionMap().entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() instanceof Collection<?>)
                    .count() > 0
        ) {
            log.warn("일부 Parameter 중에 선택되지 않은 항목이 존재합니다.", requestDomain.getJobInfo().getParameterDefinitionMap());
            restMessage.setNG();
            restMessage.setMessage("일부 Parameter 중에 선택되지 않은 항목이 존재합니다.");
            return ResponseEntity.ok(restMessage);
        }

        try {
            initBusinessToplogy();

            log.info("-- Arsenal Portal 로 부터 Jenkins DevTools 정보를 일괄로 획득합니다.");
            portalCallerService.setDevToolsInfo(requestDomain, businessTopologyDomains);

            // 요청 Domain 에서 Job정보로 Build를 수행함.
            builderService.doBuild(requestDomain);

        } catch (CustomArsenalException e) {
            restMessage.setNG();
            restMessage.setMessage(e.getMessage());
            return ResponseEntity.ok(restMessage);

        } catch (Exception e) {
            log.error("{}", e);

            restMessage.setNG();
            restMessage.setMessage(e.getMessage());
            return ResponseEntity.ok(restMessage);
        }

        restMessage.setOK();
        return ResponseEntity.ok(restMessage);
    }

    @PostMapping("/lastBuildInfo")
    public ResponseEntity<?> getLastBuildInfo(@RequestBody final BuilderJobRequestDomain requestDomain, final RestMessage restMessage, Authentication authentication) {

        // NameSpace User 체크
        if (!CommonUtil.isNamespaceUser(authentication, 1L, requestDomain.getServiceAliasName())) {
            throw new CustomArsenalException(CustomStatusCd.CD_403.getCd(), CustomStatusCd.CD_403.getMsg());
        }

        // 사업 별칭과 Jenkins view 명칭이 다른경우
        if (!requestDomain.getServiceAliasName().equals(requestDomain.getJobInfo().getViewName())) {
            log.warn("서비스 별칭과 Job의 View이름이 일치하지 않습니다. [requestDomain:{}]", requestDomain);

            restMessage.setNG();
            restMessage.setMessage("입력 데이터가 올바르지 않습니다.");
            return ResponseEntity.ok(restMessage);
        }

        // 필수 값 체크
        if ( StringUtils.isEmpty(requestDomain.getJobInfo().getName())
                || StringUtils.isEmpty(requestDomain.getJobInfo().getUrl())
                || StringUtils.isEmpty(requestDomain.getJobInfo().getViewName())
                || requestDomain.getJobInfo().getParameterDefinitionMap() == null) {
            log.warn("빌드 수행을 위한 기본값이 전달되지 않았습니다. [jobName:{}], [jobUrl:{}], [viewName:{}], [parameterMap:{}]",
                    requestDomain.getJobInfo().getUrl(),
                    requestDomain.getJobInfo().getUrl(),
                    requestDomain.getJobInfo().getViewName(),
                    requestDomain.getJobInfo().getParameterDefinitionMap());

            restMessage.setNG();
            restMessage.setMessage("빌드 수행을 위한 기본값이 전달되지 않았습니다.");
            return ResponseEntity.ok(restMessage);
        }

        LastBuildInfo lastBuildInfo;
        try {
            initBusinessToplogy();

            log.info("-- Arsenal Portal 로 부터 Jenkins DevTools 정보를 일괄로 획득합니다.");
            portalCallerService.setDevToolsInfo(requestDomain, businessTopologyDomains);

            // 요청 Domain 에서 Job정보로 최근 Build 정보를 획득함.
            lastBuildInfo = builderService.getLastBuildInfo(requestDomain);

        } catch (CustomArsenalException e) {
            restMessage.setNG();
            restMessage.setMessage(e.getMessage());
            return ResponseEntity.ok(restMessage);

        } catch (Exception e) {
            log.error("{}", e);

            restMessage.setNG();
            restMessage.setMessage(e.getMessage());
            return ResponseEntity.ok(restMessage);
        }





        restMessage.setOK();
        restMessage.setData(lastBuildInfo);
        return ResponseEntity.ok(restMessage);
    }




    @PostMapping("/buildParameters")
    public ResponseEntity<?> getBuildParameters(@RequestBody final BuilderJobRequestDomain requestDomain, final RestMessage restMessage, Authentication authentication) {

        // NameSpace User 체크
        if (!CommonUtil.isNamespaceUser(authentication, 1L, requestDomain.getServiceAliasName())) {
            throw new CustomArsenalException(CustomStatusCd.CD_403.getCd(), CustomStatusCd.CD_403.getMsg());
        }

        // 사업 별칭과 Jenkins view 명칭이 다른경우
        if (!requestDomain.getServiceAliasName().equals(requestDomain.getJobInfo().getViewName())) {
            log.warn("서비스 별칭과 Job의 View이름이 일치하지 않습니다. [requestDomain:{}]", requestDomain);

            restMessage.setNG();
            restMessage.setMessage("입력 데이터가 올바르지 않습니다.");
            return ResponseEntity.ok(restMessage);
        }

        try {
            initBusinessToplogy();

            log.info("-- Arsenal Portal 로 부터 Jenkins DevTools 정보를 일괄로 획득합니다.");
            portalCallerService.setDevToolsInfo(requestDomain, businessTopologyDomains);

            // 요청 Domain 에서 Job정보의 ParameterMap의 Value를 다시 조회 함.
            builderService.setBuildParameters(requestDomain);

        } catch (CustomArsenalException e) {
            restMessage.setNG();
            restMessage.setMessage(e.getMessage());
            return ResponseEntity.ok(restMessage);

        } catch (Exception e) {
            log.error("{}", e);

            restMessage.setNG();
            restMessage.setMessage(e.getMessage());
            return ResponseEntity.ok(restMessage);
        }

        restMessage.setOK();
        restMessage.setData(requestDomain.getJobInfo());
        return ResponseEntity.ok(restMessage);
    }



    /**
     * Jenkins Build Items 목록 정보 획득
     * @param requestDomain
     * @param restMessage
     * @param authentication
     * @return
     */
    @PostMapping("/list")
    public ResponseEntity<?> getBuilders(@RequestBody final BuilderRequestDomain requestDomain, final RestMessage restMessage, Authentication authentication) {

        // NameSpace User 체크
        if (!CommonUtil.isNamespaceUser(authentication, 1L, requestDomain.getServiceAliasName())) {
            throw new CustomArsenalException(CustomStatusCd.CD_403.getCd(), CustomStatusCd.CD_403.getMsg());
        }

        List<BuilderInfo> builderInfos = null;
        try {
            initBusinessToplogy();

            log.info("-- Arsenal Portal 로 부터 Jenkins DevTools 정보를 일괄로 획득합니다.");
            portalCallerService.setDevToolsInfo(requestDomain, businessTopologyDomains);

            builderInfos = jenkinsViewService.createViewTopology(requestDomain);

        } catch (CustomArsenalException e) {
            restMessage.setNG();
            restMessage.setMessage(e.getMessage());
            return ResponseEntity.ok(restMessage);

        } catch (Exception e) {
            log.error("Jenkins 정보 조회중 오류가 발생하였습니다.", e);
            throw new CustomArsenalException(CustomStatusCd.CD_500.getCd(), CustomStatusCd.CD_500.getMsg());
        }

        restMessage.setOK();
        restMessage.setData(builderInfos);
        return ResponseEntity.ok(restMessage);
    }


    /**
     * Business Topology 구성.
     * Portal 호출하여 정보를 구성함.
     */
    private void initBusinessToplogy() {
        // BusinessTopologyDomain 정보를 초기에 구성
        if ( businessTopologyDomains == null ) {
            log.info("-- Arsenal Portal 로 부터 Business Topology 정보를 구성합니다.");
            businessTopologyDomains = portalCallerService.initBusinessTopologyDomain();
        }
    }
}
