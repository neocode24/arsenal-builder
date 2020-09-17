package com.kt.arsenal.biz.autoenvmgmt.service;

import com.kt.arsenal.common.exception.CustomArsenalException;
import com.kt.arsenal.common.exception.CustomStatusCd;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Arsenal Gitlab Milestone 수집 서비스
 *
 * @author 82022961
 * @version 1.0.0
 * @since 2020/07/15
 */

@Slf4j
@Service
public class GitLabMilestoneService {

    @Autowired
    private GitLabService gitLabService;

    @Autowired
    private GitLabProjectService projectService;





    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MilestoneInfo {
        // milestone Id
        long id;
        // milestone version name
        String title;
        // milestone description
        String description;
        // milestone start date
        LocalDate startMilestone;
        // milestone end date
        LocalDate endMilestone;
        // web_url
        String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleasedProject {
        // Project Name
        String name;
        // Tag
        String tag;
    }


    /**
     * "active" 상태의 milestone 획득
     * @param token
     * @param uri
     * @param groupName
     * @param name
     * @return
     */
    public List<MilestoneInfo> getActiveMilestones(String token, String uri, String groupName, String name) {

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


        // closed Milestone 조건만 최근 10건만 수집 함. (수집 주기 (default:10분)에 배포 10회 초과분은 수집 제외)
        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));

        ResponseEntity<List<Map<String, Object>>> responseEntity = null;
        try {
            responseEntity = (ResponseEntity<List<Map<String, Object>>>) gitLabService.callGitLabServer(
                    uri + "/projects/" + gitProjectId + "/milestones?state=active&per_page=10",
                    HttpMethod.GET,
                    requestEntity,
                    List.class);
        } catch(Exception e) {
            log.error("[GroupName:{}], [ProjectName:{}] Error !!", groupName, name);
            return new ArrayList<>();
        }


        log.debug(" -- [GroupName:{}], [ProjectName:{}] Milestone ({})건 조회 되었습니다.", groupName, name, responseEntity.getBody().size());

        // milestone 데이터 생성 (Semantic Verstion - x.x.x 방식만 획득함)
        List<MilestoneInfo> resultList = responseEntity.getBody().stream()
                .filter(entry -> ((String)entry.get("title")).matches("^([0-9]|[1-9][0-9]*)\\.([0-9]|[1-9][0-9]*)\\.([0-9]|[1-9][0-9]*)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+)?$") )
                .map(entry -> MilestoneInfo.builder()
                        .id(new Long((Integer)entry.get("id")))
                        .title((String)entry.get("title"))
                        .description((String)entry.get("description"))
                        .startMilestone(
                                getStartMilestoneDay(token, uri, entry, gitProjectId, new Long((Integer)entry.get("id")))
                        )
                        .endMilestone(
                                getTagGeneratedDay(token, uri, entry, gitProjectId, (String)entry.get("title"))
                        )
                        .url((String)entry.get("web_url"))
                        .build()
                )
                .collect(Collectors.toList());

        return resultList;
    }




    /**
     * "closed" 상태의 milestone 획득
     * @param token
     * @param uri
     * @param groupName
     * @param name
     * @return
     */
    public List<MilestoneInfo> getClosedMilestones(String token, String uri, String groupName, String name) {

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


        // closed Milestone 조건만 최근 10건만 수집 함. (수집 주기 (default:10분)에 배포 10회 초과분은 수집 제외)
        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));

        ResponseEntity<List<Map<String, Object>>> responseEntity = null;
        try {
            responseEntity = (ResponseEntity<List<Map<String, Object>>>) gitLabService.callGitLabServer(
                    uri + "/projects/" + gitProjectId + "/milestones?state=closed&per_page=10",
                    HttpMethod.GET,
                    requestEntity,
                    List.class);
        } catch(Exception e) {
            log.error("[GroupName:{}], [ProjectName:{}] Error !!", groupName, name);
            return new ArrayList<>();
        }


        log.debug(" -- [GroupName:{}], [ProjectName:{}] Milestone ({})건 조회 되었습니다.", groupName, name, responseEntity.getBody().size());

        // milestone 데이터 생성 (Semantic Verstion - x.x.x 방식만 획득함)
        List<MilestoneInfo> resultList = responseEntity.getBody().stream()
                .filter(entry -> ((String)entry.get("title")).matches("^([0-9]|[1-9][0-9]*)\\.([0-9]|[1-9][0-9]*)\\.([0-9]|[1-9][0-9]*)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+)?$") )
                .map(entry -> MilestoneInfo.builder()
                        .id(new Long((Integer)entry.get("id")))
                        .title((String)entry.get("title"))
                        .description((String)entry.get("description"))
                        .startMilestone(
                                getStartMilestoneDay(token, uri, entry, gitProjectId, new Long((Integer)entry.get("id")))
                        )
                        .endMilestone(
                                getTagGeneratedDay(token, uri, entry, gitProjectId, (String)entry.get("title"))
                        )
                        .build()
                )
                .collect(Collectors.toList());

        return resultList;
    }


    /**
     * milestone 종료 일시 획득
     *  - 획득 기준
     *      - milestone에 일치하는 tag 명칭이 존재하는 경우 : tag commit 일자.
     *      - milestone에 일치하는 tag 명칭이 없는 경우 : milestone이 close 상태의 경우 기본적으로 tag가 생성되어 있어야 정상이나,
     *                                              예외적인 케이스로 milestone 종료일시나 업데이트 일시로 대체함.
     * @param token
     * @param uri
     * @param milestoneResponseMap
     * @param gitProjectId
     * @param milestoneTitle
     * @return
     */
    public LocalDate getTagGeneratedDay(String token, String uri, Map<String, Object> milestoneResponseMap, String gitProjectId, String milestoneTitle) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));

        // tag 전체에서 milestone으로 생성된 tag 명칭만 획득.
        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) gitLabService.callGitLabServer(
                uri + "/projects/" + gitProjectId + "/repository/tags/" + milestoneTitle,
                HttpMethod.GET,
                requestEntity,
                Map.class);

        // milestone 명칭과 일치하는 tag 명칭이 없다면, milestone의 due_date 또는 update_date 로 대체 함. (closed milestone 이기 때문에 due_date가 있다면, 종료된 일자라고 판단함)
        if ( responseEntity.getBody() != null && responseEntity.getBody().get("name") == null ) {

            LocalDate milestoneUpdatedDate  = LocalDateTime.parse((String)milestoneResponseMap.get("updated_at"), DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
            LocalDate milestoneDueDate      = milestoneResponseMap.get("due_date") == null ?  milestoneUpdatedDate : LocalDate.parse((String)milestoneResponseMap.get("due_date"), DateTimeFormatter.ISO_DATE);
            log.debug(" -- Closed Milestone({})에 일치하는 TAG가 확인되지 않아, Milestone dueDate(또는 updateDate)로 대체 합니다.", gitProjectId, milestoneTitle);

            return milestoneUpdatedDate.isBefore(milestoneDueDate) ? milestoneUpdatedDate : milestoneDueDate;
        }

        // milestone 명칭으로 생성된 tag가 생성 됨에 따라, 이름을 비교해서 찾음.
        LocalDate tagCommitedDate = LocalDateTime.parse((String) ((Map<String, Object>) responseEntity.getBody().get("commit")).get("committed_date"), DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
        log.debug(" -- Closed Milestone({}) - Tag CommitedDate({}) 명칭의 Commit 일자로 종료일자를 산출합니다.", milestoneTitle, tagCommitedDate);

        return tagCommitedDate;
    }

    /**
     * milestone 시작일시 획득
     *  - 획득 기준
     *      - milestone에 issue가 있는 경우 : issue 중에서 가장 오래된 일자
     *      - milestone에 issue가 없는 경우 : milestone에 지정된 시작일자 또는 생성일자 중에 오래된 일자
     * @param token
     * @param uri
     * @param milestoneResponseMap
     * @param gitProjectId
     * @param milestoneId
     * @return
     */
    public LocalDate getStartMilestoneDay(String token, String uri, Map<String, Object> milestoneResponseMap, String gitProjectId, Long milestoneId) {

        final HttpEntity<?> requestEntity = new HttpEntity<>(gitLabService.createHeader(token));

        // milestone 에서 issue 리스트를 획득.
        ResponseEntity<List<Map<String, Object>>> responseEntity = (ResponseEntity<List<Map<String, Object>>>) gitLabService.callGitLabServer(
                uri + "/projects/" + gitProjectId + "/milestones/" + milestoneId + "/issues",
                HttpMethod.GET,
                requestEntity,
                List.class);


        String milestoneTitle = (String) milestoneResponseMap.get("title");

        // issue 가 존재하면, issue 생성일자를 milestone 시작일자로...
        if ( responseEntity.getBody() != null && responseEntity.getBody().size() > 0 ) {

            // issue 중 가장 최근 생성된 issue 확인.
            Map<String, Object> firstIssue = responseEntity.getBody().stream()
                    .min(
                            Comparator.comparing(entry -> LocalDateTime.parse((String) entry.get("created_at"), DateTimeFormatter.ISO_DATE_TIME))
                    )
                    .orElseGet(HashMap::new);

            log.debug(" -- Started Milestone({}) - Issue 중 가장 최초 생성일자를 산출합니다.", milestoneTitle);
            return LocalDateTime.parse((String)firstIssue.get("created_at"), DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
        }


        // issue 가 없으면, milestone 시작일자와 생성일자 중 가장 빠른일자로
        LocalDate milestoneCreateDate   = LocalDateTime.parse((String)milestoneResponseMap.get("created_at"), DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
        LocalDate milestoneStartDate    = milestoneResponseMap.get("start_date") == null ? milestoneCreateDate : LocalDate.parse((String) milestoneResponseMap.get("start_date"), DateTimeFormatter.ISO_DATE);
        log.debug(" -- Started Milestone({}) - startDate(또는 createDate)로 대체 합니다.", milestoneTitle);

        return milestoneCreateDate.isBefore(milestoneStartDate) ? milestoneCreateDate : milestoneStartDate;
    }

}
