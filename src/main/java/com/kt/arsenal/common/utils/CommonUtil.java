/*
 * Arsenal-Platform version 1.0
 * Copyright ⓒ 2019 kt corp. All rights reserved.
 * This is a proprietary software of kt corp, and you may not use this file except in
 * compliance with license agreement with kt corp. Any redistribution or use of this
 * software, with or without modification shall be strictly prohibited without prior written
 * approval of kt corp, and the copyright notice above does not evidence any actual or
 * intended publication of such software.
 */
package com.kt.arsenal.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kt.arsenal.common.constant.CodeConstUtil;
import com.kt.arsenal.common.constant.ConstUtil;
import com.kt.arsenal.common.exception.CustomArsenalException;
import com.kt.arsenal.common.exception.CustomStatusCd;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * ARSENAL 공통 유틸리티
 * @author
 * @since .
 * @version 1.0.0
 * @see
 */
@Slf4j
@Component
public class CommonUtil {

    @Autowired
    private MessageSource messageSource;

    private ObjectMapper mapper;

    /**
     * Date Format 형식의 날짜 문자열(Date String)을 Date 객체로 변환
     * @param dataformat(ex:"yyyy/MM/dd HH:mm:ss"), dateString(ex:"9999/12/31 00:00:00")
     * @return
     */
    public Date convertStrToDate(String dataformat, String dateString) {
        try {
            return new SimpleDateFormat(dataformat, Locale.KOREA).parse(dateString);
        } catch (ParseException ex) {
            throw new CustomArsenalException(CustomStatusCd.CD_500.getCd(), CustomStatusCd.CD_500.getMsg(), ex);
        }
    }


    /**
     * Date 객체를 지정된 날짜 포맷 형식의 문자열로 변환
     * @param dataformat, date
     * @return
     */
    public String convertDateToStr(String dataformat, Date date) {
        return new SimpleDateFormat(dataformat, Locale.KOREA).format(date);
    }


    /**
     * Date객체를 시작 일시로 설정
     * @param date
     * @return
     */
    public Date atStartOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
        return localDateTimeToDate(startOfDay);
    }


    /**
     * Date객체를 종료 일시로 설정
     * @param date
     * @return
     */
    public Date atEndOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
        return localDateTimeToDate(endOfDay);
    }


    /**
     * Date객체를 LocalDateTime객체로 변환
     * @param date
     * @return
     */
    public LocalDateTime dateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }


    /**
     * LocalDateTime객체를 Date객체로 변환
     * @param localDateTime
     * @return
     */
    public Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }


    /**
     * 검색 시작 일 설정
     * @param dateStr, startDays
     * @return
     * @throws ParseException
     */
    public String getSearchStartDateStr(String dateStr, int addDays) {
        return StringUtils.isBlank(dateStr)
                ? convertDateToStr(ConstUtil.MYSQL_DATE_FORMAT, atStartOfDay(DateUtils.addDays(new Date(), -addDays))) : dateStr;
    }


    /**
     * 검색 종료 일 설정
     * @param dateStr, startDays
     * @return
     * @throws ParseException
     */
    public String getSearchEndDateStr(String dateStr, int addDays) {
        return StringUtils.isBlank(dateStr)
                ? convertDateToStr(ConstUtil.MYSQL_DATE_FORMAT, atStartOfDay(DateUtils.addDays(new Date(), addDays))) : dateStr;
    }


    /**
     * 검색 시작 일 설정(시작 일 00:00)
     * @param date, addDays
     * @return
     */
    public Date getSearchStartDate(Date date, int addDays) {
        return date != null ? atStartOfDay(date) : atStartOfDay(DateUtils.addDays(new Date(), addDays));
    }


    /**
     * 검색 종료 일 설정(사용자 입력 일자) / 초단위 절사
     * @param date, addDays
     * @return
     */
    public Date getSearchEndDate(Date date) {
        return date != null ? DateUtils.truncate(date, Calendar.MINUTE) : DateUtils.truncate(new Date(), Calendar.MINUTE);
    }


    /**
     * 총 경과 시간(millis)을 시간(H)과 분(M)으로 표시
     * @param start, end
     * @return
     */
    public String getFormatTimeByMillis(Date start, Date end) {
        return (end == null || end.equals(convertStrToDate(ConstUtil.MYSQL_DATE_FORMAT, ConstUtil.NULL_TO_DATE))) ? getFormatTimeByMillis(Duration.between(dateToLocalDateTime(start), dateToLocalDateTime(new Date())).toMillis())
                : getFormatTimeByMillis(Duration.between(dateToLocalDateTime(start), dateToLocalDateTime(end)).toMillis());
    }


    /**
     * 총 경과 시간(MILLISECONDS)을 시간(H), 분(m), 초(s)로 표시
     * @param millis
     * @return
     */
    public String getFormatTimeByMillis(long millis) {
        String h = TimeUnit.MILLISECONDS.toHours(millis) + "시";
        String m = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1) + "분";
        String s = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1) + "초";
        return (!"0시".equals(h) ? h : "") + (!"0분".equals(m) ? m : "") + (!"0초".equals(s) ? s : "");
    }


    /**
     * 특정 날짜가가 조회 기간 사이에 있는지 확인(startDate < specificDate < endDate)
     * @param specificDate, startDate, endDate
     * @return
     */
    public boolean isWithinRange(Date specificDate, Date startDate, Date endDate) {
        //return !(specificDate.before(startDate) || specificDate.after(endDate)); // startDate <= specificDate <= endDate
        return specificDate.after(startDate) && specificDate.before(endDate); // startDate < specificDate < endDate
    }


    /**
     * 특정 날짜가 조회 기간 이전에 있는지 확인(specificDate <= [startDate ~ endDate])
     * @param specificDate, startDate
     * @return
     */
    public boolean isLessThanOrEqual(Date specificDate, Date startDate) {
        return specificDate.compareTo(startDate) <= 0;
    }


    /**
     * 특정 날짜가 조회 기간 이후에 있는지 확인([startDate ~ endDate] <= specificDate)
     * @param specificDate, endDate
     * @return
     */
    public boolean isGreaterThanOrEqual(Date specificDate, Date endDate) {
        return specificDate.compareTo(endDate) >= 0;
    }


    /**
     * 두개의 같은 타입의 리스트를 통합
     * @param first, second
     * @return
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> unionList(List<? extends E> first, List<? extends E> second) {
        return ListUtils.union(first == null ? Collections.emptyList() : first, second == null ? Collections.emptyList() : second);
    }


    /**
     * 실행 중인 서버의 IP
     * @return
     */
    public String getServerIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            throw new CustomArsenalException(CustomStatusCd.CD_500.getCd(), CustomStatusCd.CD_500.getMsg(), ex);
        }
    }


    /**
     * 사용자 IP
     * @param request
     * @return
     */
    public String getClientIp(HttpServletRequest request) {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }


    /**
     * 리스트 오브젝트 안에 특정한 프로퍼티의 값이 중복되는 오브젝트를 리스트에서 제거
     * @param keyExtractor
     * @return
     */
    public <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }


    /**
     * 프러퍼티 파일에서 메시지 획득
     * @param message
     * @return
     */
    public String getMessage(String message){
        return messageSource.getMessage(message, null, Locale.KOREAN);
    }



    /**
     * 엑셀 파일명 생성(한글 처리 가능)
     * @param request, filename
     * @return
     */
    public String generateDownloadFilename(HttpServletRequest request, String filename) {
        String browser = request.getHeader("User-Agent");
        String attachFilename = "";

        try {

            if (browser.contains("MSIE") || browser.contains("Trident") || browser.contains("Chrome")) {
                attachFilename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
            } else {
                attachFilename = new String(filename.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            }

            return attachFilename + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern(ConstUtil.LOG_DATE_FORMAT));

        } catch (UnsupportedEncodingException ex) {
            throw new CustomArsenalException(CustomStatusCd.CD_91300.getCd(), CustomStatusCd.CD_91300.getMsg(), ex);
        }
    }


    /**
     * 파일 저장 여러건
     * @param saveLocation, filename
     * @return
     */
    public void storeFiles(MultipartFile[] files, String saveLocation) {
        for (MultipartFile file : files) {

            if (file.isEmpty()) {
                continue; // next pls
            }

            File dir = new File(saveLocation);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            try {
                Files.copy(file.getInputStream(), Paths.get(saveLocation + file.getOriginalFilename()), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ex) {
                throw new CustomArsenalException(CustomStatusCd.CD_91100.getCd(), CustomStatusCd.CD_91100.getMsg(), ex);
            }
        }

    }

    /**
     * 파일 저장 1건
     * @param saveLocation, filename
     * @return
     */
    public void storeFile (MultipartFile file, String saveLocation, String saveFilename) {

        if (file.isEmpty()) {
            return;
        }

        File dir = new File(saveLocation);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            Files.copy(file.getInputStream(), Paths.get(saveLocation + saveFilename), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            throw new CustomArsenalException(CustomStatusCd.CD_91100.getCd(), CustomStatusCd.CD_91100.getMsg(), ex);
        }

    }

    /**
     * 파일 삭제 1건
     * @param saveLocation, filename
     * @return
     */
    public void deleteFile (String saveLocation, String deleteFilename) {
        try {
            Files.deleteIfExists(Paths.get(saveLocation + deleteFilename));
        } catch (Exception ex) {
            throw new CustomArsenalException(CustomStatusCd.CD_91200.getCd(), CustomStatusCd.CD_91200.getMsg(), ex);
        }
    }


    /**
     * 파일 용량 계산
     * @param uploadMaxSize
     * @return
     */
    public long calculateUploadSize (String uploadMaxSize) {

        log.debug("uploadMaxSize :::: " + uploadMaxSize);
        String numberValue = uploadMaxSize.replaceAll("\\D", "");
        log.debug("numberValue :::: " + numberValue);
        String stringValue = uploadMaxSize.replaceAll("\\d", "");
        log.debug("stringValue :::: " + stringValue);


        int stringToIntValue = 0;
        if (numberValue != null && !"".equals(stringValue)) {
            stringToIntValue = Integer.parseInt(numberValue);
        }

        long calculatedMaxSize = 0;
        if (stringValue != null && !"".equals(stringValue)) {
            switch (stringValue.toUpperCase()) {
                case "KB" :
                    calculatedMaxSize = (long) stringToIntValue * 1024;
                    break;
                case "MB" :
                    calculatedMaxSize = (long) stringToIntValue * 1024 * 1024;
                    break;
                case "GB" :
                    calculatedMaxSize = (long) stringToIntValue * 1024 * 1024 * 1024;
                    break;
                default:
                    calculatedMaxSize = stringToIntValue;
                    break;
            }
        }

        return calculatedMaxSize;

    }


    /**
     * API 호출 시 Date 포맷 변환
     * [MYSQL_DATE_FORMAT = yyyy/MM/dd HH:mm:ss]
     * 82107478 추가 (2019.01.25)
     * @param dateString
     * @return String
     */
    public String convertDateFormat(String dateString) {
        Instant timestamp = Instant.parse(dateString);
        ZonedDateTime zoneTime = timestamp.atZone(ZoneId.of("Asia/Tokyo"));
        return zoneTime.format(DateTimeFormatter.ofPattern(ConstUtil.MYSQL_DATE_FORMAT));
    }

    /**
     * String to date
     * 82107247
     * @param dateString
     * @return Date
     * @throws ParseException
     */
    public Date convertStringToDate(String dateString, String format) throws ParseException {
        SimpleDateFormat trans = new SimpleDateFormat(format);
        return trans.parse(dateString);
    }


    /**
     * get file mediatype
     * @param servletContext
     * @param fileName
     * @return
     */
    public MediaType getMediaTypeForFileName(ServletContext servletContext, String fileName) {
        try {
            String mineType = servletContext.getMimeType(fileName);
            return MediaType.parseMediaType(mineType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * DEV Arsenal-Portal 호출 서비스
     * @param restTemplate RestTemplate
     * @param uri URI
     * @param httpMethod Http Method
     * @param httpEntity Request Entity
     * @param responseClass Response Class
     * @param <T> Response Class Generic
     * @return Response
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> ResponseEntity<T> callDevArsenalPortal(RestTemplate restTemplate, String uri, HttpMethod httpMethod, HttpEntity httpEntity, Class responseClass) {
        return restTemplate.exchange(uri, httpMethod, httpEntity, responseClass);
    }

    /**
     * DEV Arsenal-Portal을 호출하기 위한 Header (Token 추가 로직)
     * @param token Arsenal-Portal Token
     * @return
     */
    public HttpHeaders createDevArsenalHeader(String token) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", token);

        return headers;
    }


    /**
     * Yaml Object(Map<String, Object>)에 대해 key(properties 형태)로 요청해서 찾는 함수
     *
     * ---[Yaml Sample]--------------------------------------------------------------
     * kafka:
     *   replicas: 1
     *   topics:
     *   - name: test1
     *     partitions: 1
     *     replicationFactor: 1
     *     reassignPartitions: false
     *   - name: test2
     *      partitions: 2
     *      replicationFactor: 2
     *      reassignPartitions: true
     * ---[Yaml Sample]--------------------------------------------------------------
     *
     * 예1) key : kafka.replicas, value : new Yaml()
     *      - 결과 : 1
     * 예2) key : kafka.topics[]
     *      - 결과 : [{"name":"test1", "partitions":1, "replicationFactor":1, "reassignPartitions":false"}, {"name":"test2", "partitions":2, "replicationFactor":1, "reassignPartions":false"}]
     *               topics 하위의 List 제공
     * 예3) key : kafka.topics[].name
     *      - 결과 : ["test1", "test2"]
     *               topics 하위의 List 중에 name 만 List 로 제공
     *               [] 배열을 사용한 경우 배열을 기준으로 1레벨만 가능
     *
     * @param key
     * @param source
     * @return
     */
    public Object getValueInYamlFormatted(String key, Map<String, Object> source) {
        return setValueInYamlFormatted(key, source, null);
    }

    /**
     * Yaml Object(Map<String, Object>)에 대한 key로 value를 변경하는 함수
     *
     * 예1) commonUtil.setValueInYamlFormatted("kafka.replicas", map, 3);
     *      - 결과 : map 으로 구성된 yaml에 "kafka.replicas" value를 3으로 변경
     *
     * 예2) commonUtil.setValueInYamlFormatted("kafka.topics", map, Arrays.asList(
     *          new HashMap<String, Object>() {
     *              {
     *                  put("name", "test2");
     *                  put("partitions", 2);
     *                  put("replicationFactor", 2);
     *                  put("reassignPartitions", false);
     *              }
     *          },
     *          new HashMap<String, Object>() {
     *              {
     *                  ...
     *              }
     *          }));
     *      - 결과 : map 으로 구성된 yaml에 "kafka.topics" value를 List로 변경
     *               (기존 value가 List가 아닌 일반 Object 이었어도, List로 변경됨)
     *
     * 예3) commonUtil.setValueInYamlFormatted("kafka.topics[]", map, new HashMap<String, Object>() {
     *          {
     *              put("name", "test2");
     *              put("partitions", 2);
     *              put("replicationFactor", 2);
     *              put("reassignPartitions", false);
     *          });
     *      - 결과 : map 으로 구성된 yaml에 "kafka.topics" 배열의 마지막에 새로운 Item(Map)을 추가로 넣음.
     *
     * 예4) commonUtil.setValueInYamlFormatted("kafka.topics[2]", map, new HashMap<String, Object>() {
     *          {
     *              put("name", "test2");
     *              put("partitions", 2);
     *              put("replicationFactor", 2);
     *              put("reassignPartitions", false);
     *          });
     *      - 결과 : map 으로 구성된 yaml에 "kafka.topics" 배열의 인덱스에 새로운 Item(Map)으로 변경함.
     *
     * 예5) commonUtil.setValueInYamlFormatted("ingress.hosts[0].host", map, "test.arsenal.kt.co.kr"
     *
     *      - 결과 : map 으로 구성된 yaml에 "ingress.hosts"에 배열을 만들어서 0번째에 "host" 를 key로 갖고, "test.arsenal.kt.co.kr"을 value로 생성한다.
     *
     *
     * 예6) commonUtil.setValueInYamlFormatted("ingress.hosts[0].paths[0], map, "/"
     *
     *      - 결과 : map 으로 구성된 yaml에 "ingress.hosts[0].paths"에 배열을 만들어서 0번째에 "/"를 value로 생성한다.
     *
     * @param key
     * @param source
     * @param value
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object setValueInYamlFormatted(String key, Map<String, Object> source, Object value) {
        String[] splitedKey = key.split("\\.");

        if (splitedKey.length > 1) {

            String subKey = splitedKey[0];
            int arrIdx = -1;

            int arrIdx0 = splitedKey[0].indexOf("[");
            int arrIdx1 = splitedKey[0].indexOf("]");
            if (arrIdx0 > -1 && arrIdx1 > -1) {
                subKey = subKey.substring(0, arrIdx0);

                // 배열 idx를 지정하지 않고 []로 명시한 경우.
                if (arrIdx1 - arrIdx0 == 1 || splitedKey[0].substring(arrIdx0 + 1, arrIdx1).trim().length() == 0 ) {
                    arrIdx = -99;
                }
                // 배열 idx를 지정한 경우
                else {
                    arrIdx = Integer.parseInt(splitedKey[0].substring(arrIdx0 + 1, arrIdx1));
                }
            }

            String nextKey = String.join(".", Arrays.copyOfRange(splitedKey, 1, splitedKey.length));
            // 배열 idx지정없이 다음 token에 맞춰서 배열을 희망할 때
            if ( arrIdx == -99 ) {
                List list = (List)source.get(subKey);
                List nextList = new ArrayList<>();
                for (int i = 0 ; i < list.size() ; i++) {
                    Object nextObject = ((Map<String, Object>)list.get(i)).get(nextKey);
                    if ( nextObject != null ) {
                        nextList.add(nextObject);
                    }
                }
                return nextList;
            }
            // 특정 배열 idx에 해당하는 값을 계속 찾아 갈 때
            else if ( arrIdx >= 0 ) {
                List list = (List)source.get(subKey) == null ? new ArrayList<Map<String, Object>>(arrIdx + 1) : (List)source.get(subKey);

                // 배열 idx 초과 여부 확인. 범위 내면 search recursive
                if ( arrIdx < list.size() ) {
                    Object subValue = list.get(arrIdx);
                    return setValueInYamlFormatted(nextKey, (Map<String, Object>) subValue, value);
                }
                // 범위 외인경우 list를 추가 생성함.
                else {
                    Map<String, Object> subValue  = new HashMap<>();
                    subValue.put(nextKey, value);
                    list.add(arrIdx, subValue);

                    // 이전 값과의 계층 관계 형성
                    source.put(subKey, list);

                    return setValueInYamlFormatted(nextKey, subValue, value);
                }
            }
            // 배열이 아닌, 지정 값(Map)인 경우
            else {

                // get Entry가 존재하지 않고 set 하는 경우, Entry를 생성하고 재귀를 다시 수행함.
                if ( source.get(subKey) == null && value != null ) {
                    source.put(subKey, new HashMap<String, Object>());
                }

                return setValueInYamlFormatted(nextKey, (Map<String, Object>)source.get(subKey), value);
            }
        }

        // GET 으로 검색
        if ( value == null ) {
            return source == null ? null : source.get(splitedKey[0]);
        }
        // SET 으로 변경
        else {


            int arrIdx0 = splitedKey[0].indexOf("[");
            int arrIdx1 = splitedKey[0].indexOf("]");
            if (arrIdx0 > -1 && arrIdx1 > -1) {
                String subKey = splitedKey[0].substring(0, arrIdx0);

                // 배열 idx를 지정하지 않고 []로 명시하여 변경하는 경우. 배열 마지막에 추가 함.
                if (arrIdx1 - arrIdx0 == 1 || splitedKey[0].substring(arrIdx0 + 1, arrIdx1).trim().length() == 0 ) {
                    List list = (List)source.get(subKey);

                    List newList = new ArrayList<>();
                    newList.addAll(list);
                    newList.add(value);
                    source.put(subKey, newList);

                    return newList;
                }
                // 배열 idx를 지정하여 변경하는 경우. 해당 위치에 값을 대체함.
                else {
                    int arrIdx = Integer.parseInt(splitedKey[0].substring(arrIdx0 + 1, arrIdx1));

                    List list = null;
                    List newList = new ArrayList<>();

                    // 지정된 배열에 값이 없는 경우 신규 객체 할당.
                    if ((list = (List) source.get(subKey)) == null) {
                        list = new ArrayList<Map<String, Object>>(arrIdx + 1);
                    }
                    // 이미 있는 경우 새로운 객체에 저장하고, 지정된 index를 제거함.
                    else {
                        newList.addAll(list);
                        newList.remove(arrIdx);
                    }
                    // 새로운 index에 추가.
                    newList.add(arrIdx, value);
                    source.put(subKey, newList);

                    return newList;
                }
            }

            Boolean.parseBoolean(String.valueOf(value));

            String strValue = String.valueOf(value);
            // boolean type
            if ( strValue.equalsIgnoreCase("true") | strValue.equalsIgnoreCase("false") ) {
                source.put(splitedKey[0], Boolean.parseBoolean(strValue));
                return Boolean.parseBoolean(strValue);
            }
            // numberic type
            else if (NumberUtils.isCreatable(strValue)) {
                source.put(splitedKey[0], NumberUtils.createNumber(strValue));
                return NumberUtils.createNumber(strValue);
            }
            // string type
            else {
                source.put(splitedKey[0], value);
                return value;
            }
        }
    }

    /**
     * Json Pretty Formatter
     * 바이너리(byte[]) 인 경우 "[byte array ...]" 문구로 대체 출력함.
     *
     * @param object Json Object or Json String
     * @return Pretty formated text
     */
    public String getPrettyJsonFormat(Object object) {

        String jsonFormatStr = "";

        try {
            if ( mapper == null ) {
                mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            }

            if (object instanceof byte[]) {
                return "[byte array ...(length:" + ((byte[])(object)).length  + ")]";
            }

            // Json PrettyFormat으로 변환. -> 실패인 경우, JsonProcessingException 발생
            jsonFormatStr = mapper.writeValueAsString(object);

        } catch (JsonProcessingException e) {
            jsonFormatStr = object.toString();
        }

        return jsonFormatStr;
    }

    /**
     * 슈퍼 어드민(Super Admin) 여부
     * @param authentication
     * @return
     */
    public static boolean isSuperAdmin(Authentication authentication) {
        for (GrantedAuthority auth : ((UserDetails)authentication.getPrincipal()).getAuthorities()) {
            if (CodeConstUtil.SUPER_USER.equals(auth.getAuthority())) return true;
        }
        return false;
    }

    /**
     * 클러스터 관리자 여부 확인
     * 클러스터 관리자는 다음을 포함한다.
     * (슈퍼ADMIN, 클러스터ADMIN)
     * @param authentication
     * @param clusterMasterId
     * @return
     */
    public static boolean isClusterAdmin(Authentication authentication, Long clusterMasterId) {

        for (GrantedAuthority auth : ((UserDetails)authentication.getPrincipal()).getAuthorities()) {
            if (CodeConstUtil.SUPER_USER.equals(auth.getAuthority()) ||
                    (clusterMasterId.toString() + "_" + CodeConstUtil.CLUSTER_ADMIN).equals(auth.getAuthority())
            ) return true;
        }
        return false;
    }

    /**
     * 클러스터 사용자 여부 확인
     * 클러스터 사용자는 다음을 포함한다.
     * (슈퍼ADMIN, 클러스터ADMIN, 클러스터USER)
     * @param authentication
     * @param clusterMasterId
     * @return
     */
    public static boolean isClusterUser(Authentication authentication, Long clusterMasterId) {

        for (GrantedAuthority auth : ((UserDetails)authentication.getPrincipal()).getAuthorities()) {
            if (CodeConstUtil.SUPER_USER.equals(auth.getAuthority()) ||
                    (clusterMasterId.toString() + "_" + CodeConstUtil.CLUSTER_ADMIN).equals(auth.getAuthority()) ||
                    (clusterMasterId.toString() + "_" + CodeConstUtil.CLUSTER_USER).equals(auth.getAuthority())
            ) return true;
        }
        return false;
    }


    /**
     * 네임스페이스 관리자 여부 확인
     * 네임스페이스 관리자는 다음을 포함한다.
     * (슈퍼ADMIN, 클러스터ADMIN, 네임스페이스ADMIN)
     * @param authentication
     * @param clusterMasterId
     * @param namespace
     * @return
     */
    public static boolean isNamespaceAdmin(Authentication authentication, Long clusterMasterId, String namespace) {

        for (GrantedAuthority auth : ((UserDetails)authentication.getPrincipal()).getAuthorities()) {
            if (CodeConstUtil.SUPER_USER.equals(auth.getAuthority()) ||
                    (clusterMasterId.toString() + "_" + CodeConstUtil.CLUSTER_ADMIN).equals(auth.getAuthority()) ||
                    (clusterMasterId.toString() + "_" + namespace + "_" + CodeConstUtil.NAMESPACE_ADMIN).equals(auth.getAuthority())) return true;
        }
        return false;
    }


    /**
     * 네임스페이스 사용자 여부 확인
     * 네임스페이스 사용자는 다음을 포함한다.
     * (슈퍼ADMIN, 클러스터ADMIN, 클러스터USER, 네임스페이스ADMIN, 네임스페이스USER)
     * @param authentication
     * @param clusterMasterId
     * @param namespace
     * @return
     */
    public static boolean isNamespaceUser(Authentication authentication, Long clusterMasterId, String namespace) {

        for (GrantedAuthority auth : ((UserDetails)authentication.getPrincipal()).getAuthorities()) {
            if (CodeConstUtil.SUPER_USER.equals(auth.getAuthority()) ||
                    (clusterMasterId.toString() + "_" + CodeConstUtil.CLUSTER_ADMIN).equals(auth.getAuthority()) ||
                    (clusterMasterId.toString() + "_" + CodeConstUtil.CLUSTER_USER).equals(auth.getAuthority()) ||
                    (clusterMasterId.toString() + "_" + namespace + "_" + CodeConstUtil.NAMESPACE_ADMIN).equals(auth.getAuthority()) ||
                    (clusterMasterId.toString() + "_" + namespace + "_" + CodeConstUtil.NAMESPACE_USER).equals(auth.getAuthority())
            )
                return true;
        }
        return false;
    }


    /**
     * Input으로 요청한 수 만큼 피보나치 수열 순서가 포함된 List를 반환.
     * Loop 반복 횟수 조건으로 사용하기 위해서, 피보나치 수열은 0, 1 부터가 아닌, 1, 2부터 시작 함.
     *
     * @param size 최대 수
     * @return [1, 2, 3, 5, 8, 13, 21, 34, 55, 89]
     */
    public static List<Integer> getFibonacciLoopSize(int size) {

        return Stream.iterate(new int[]{1, 2}, t -> new int[]{t[1], t[0] + t[1]})
                .limit(size)
                .map(t -> t[0])
                .collect(Collectors.toList());
    }
}
