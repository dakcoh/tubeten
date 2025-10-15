package com.tubeten.ten.api.popularvideo.service;

import com.tubeten.ten.api.popularvideo.dto.PopularVideoResponse;
import com.tubeten.ten.config.YoutubeApiConfig;
import com.tubeten.ten.exception.ErrorCode;
import com.tubeten.ten.exception.TubetenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularTopQueryService {
    private final YoutubeApiConfig youtubeApiConfig;
    private final RestTemplate restTemplate;

    @Cacheable(cacheNames="top",
            key="T(java.lang.String).format('v2:%s:%s:%s',(#region==null?'KR':#region.toUpperCase()),((#categoryId==null||#categoryId.isEmpty())?'all':#categoryId),#size)",
            sync=true
    )
    public List<PopularVideoResponse> getCurrentTop(String region, String categoryId, int size) {
        try {
            // 파라미터 검증
            validateParameters(region, categoryId, size);
            
            String r = region == null ? "KR" : region.toUpperCase();
            String c = (categoryId == null || categoryId.isBlank()) ? null : categoryId.trim();
            int n = Math.max(1, Math.min(size, 200));
            
            log.debug("YouTube API 호출 - region: {}, category: {}, size: {}", r, c, n);
            List<PopularVideoResponse> list = fetchYoutubeTop100(r, c);
            log.debug("YouTube API 완료 - 결과: {}개", list.size());
            
            return list.size() > n ? new ArrayList<>(list.subList(0, n)) : list;
            
        } catch (TubetenException e) {
            log.error("YouTube 인기 영상 조회 실패 - region: {}, category: {}, error: {}", 
                    region, categoryId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("YouTube 인기 영상 조회 중 예상치 못한 오류 - region: {}, category: {}", 
                    region, categoryId, e);
            throw new TubetenException(ErrorCode.YOUTUBE_API_ERROR, "인기 영상 조회 중 오류가 발생했습니다", e);
        }
    }

    private List<PopularVideoResponse> fetchYoutubeTop100(String r, String c) {
        List<PopularVideoResponse> out = new ArrayList<>(100);
        String pageToken = null;
        
        try {
            // 최대 10페이지까지 시도 (YouTube API 제한까지 최대한 가져오기)
            int maxPages = 10;
            for (int page = 0; page < maxPages; page++) {
                var b = UriComponentsBuilder
                        .fromUri(URI.create(youtubeApiConfig.getBaseUrl() + "/videos"))
                        .queryParam("part", "snippet,statistics,contentDetails")
                        .queryParam("chart", "mostPopular")
                        .queryParam("regionCode", r)
                        .queryParam("maxResults", 50)
                        .queryParam("key", youtubeApiConfig.getKey());
                
                // videoCategoryId는 chart=mostPopular와 함께 사용할 수 없으므로 서버에서 필터링
                if (pageToken != null) b.queryParam("pageToken", pageToken);

                String url = b.toUriString();
                log.debug("YouTube API 요청 (페이지 {}) - region: {}, category: {}", page + 1, r, c);
                
                ResponseEntity<com.fasterxml.jackson.databind.JsonNode> res =
                        restTemplate.getForEntity(url, com.fasterxml.jackson.databind.JsonNode.class);
                        
                var body = res.getBody();
                if (body == null) {
                    log.warn("YouTube API 응답 body가 null입니다");
                    throw new TubetenException(ErrorCode.YOUTUBE_API_INVALID_RESPONSE, "API 응답이 비어있습니다");
                }
                
                // 에러 응답 체크
                if (body.has("error")) {
                    var error = body.get("error");
                    String errorMessage = error.has("message") ? error.get("message").asText() : "알 수 없는 오류";
                    int errorCode = error.has("code") ? error.get("code").asInt() : 0;
                    
                    log.error("YouTube API 에러 응답 - code: {}, message: {}", errorCode, errorMessage);
                    
                    if (errorCode == 403) {
                        throw new TubetenException(ErrorCode.YOUTUBE_API_QUOTA_EXCEEDED, errorMessage);
                    } else {
                        throw new TubetenException(ErrorCode.YOUTUBE_API_ERROR, errorMessage);
                    }
                }
                
                var items = body.get("items");
                if (items == null || !items.isArray()) {
                    log.warn("YouTube API 응답에 items가 없거나 배열이 아닙니다 - 페이지: {}", page + 1);
                    break;
                }
                
                log.debug("페이지 {} - 받은 아이템 수: {}, 총 누적: {}", page + 1, items.size(), out.size());
                
                // 응답 데이터 파싱 및 카테고리 필터링
                for (var item : items) {
                    try {
                        PopularVideoResponse video = PopularVideoResponse.from(item);
                        
                        // 카테고리 필터링 (c가 null이면 모든 카테고리 포함)
                        if (c == null || c.equals(video.categoryId())) {
                            out.add(video);
                        } else if ("24".equals(c) && video.shorts()) {
                            out.add(video);
                        }
                    } catch (Exception e) {
                        log.warn("비디오 데이터 파싱 실패", e);
                    }
                }
                
                pageToken = body.has("nextPageToken") ? body.get("nextPageToken").asText(null) : null;
                if (pageToken == null) {
                    log.debug("더 이상 페이지가 없습니다. 최종 수집된 데이터: {}개 (region: {}, category: {})", out.size(), r, c);
                    break;
                }
                
                log.debug("다음 페이지 토큰 존재 - 계속 진행 (현재: {}개)", out.size());
                
                int targetSize = (c != null) ? 100 : 200;
                if (out.size() >= targetSize) {
                    log.info("충분한 데이터를 확보했습니다: {}개 - 조기 종료 (카테고리: {})", out.size(), c);
                    break;
                }
            }
            
            log.info("YouTube API 호출 성공 - region: {}, category: {}, 결과 수: {}", r, c, out.size());
            return out.size() > 200 ? out.subList(0, 200) : out;
            
        } catch (TubetenException e) {
            throw e;
        } catch (Exception e) {
            log.error("YouTube API 호출 중 예상치 못한 오류 - region: {}, category: {}", r, c, e);
            throw new TubetenException(ErrorCode.YOUTUBE_API_ERROR, "YouTube API 호출 실패", e);
        }
    }
    
    /**
     * 확장된 데이터 수집을 위한 메서드
     */
    @Cacheable(cacheNames="top",
            key="T(java.lang.String).format('v2:ext:%s:%s:%s',(#region==null?'KR':#region.toUpperCase()),((#categoryId==null||#categoryId.isEmpty())?'all':#categoryId),#targetSize)",
            sync=true
    )
    public List<PopularVideoResponse> getExtendedTop(String region, String categoryId, int targetSize) {
        try {
            log.debug("확장 데이터 수집 - region: {}, category: {}, target: {}", region, categoryId, targetSize);
            
            List<PopularVideoResponse> result = new ArrayList<>(fetchYoutubeTop100(region, categoryId));
            log.debug("1차 조회: {}개", result.size());
            
            if (result.size() >= targetSize) {
                return new ArrayList<>(result.subList(0, Math.min(targetSize, result.size())));
            }
            if (result.size() < targetSize && !"US".equals(region)) {
                try {
                    List<PopularVideoResponse> usData = fetchYoutubeTop100("US", categoryId);
                    Set<String> existingIds = result.stream()
                            .map(PopularVideoResponse::videoId)
                            .collect(java.util.stream.Collectors.toSet());
                    
                    for (PopularVideoResponse video : usData) {
                        if (!existingIds.contains(video.videoId()) && result.size() < targetSize) {
                            // 카테고리 필터링 적용
                            if (categoryId == null || categoryId.equals(video.categoryId())) {
                                result.add(video);
                                existingIds.add(video.videoId());
                            }
                        }
                    }
                    log.debug("2차 US 데이터: {}개", result.size());
                } catch (Exception e) {
                    log.warn("US 데이터 추가 실패", e);
                }
            }
            

            if (result.size() < targetSize && categoryId != null) {
                try {
                    List<PopularVideoResponse> globalData = fetchYoutubeTop100("US", categoryId);
                    Set<String> existingIds = result.stream()
                            .map(PopularVideoResponse::videoId)
                            .collect(java.util.stream.Collectors.toSet());
                    
                    for (PopularVideoResponse video : globalData) {
                        if (!existingIds.contains(video.videoId()) && result.size() < targetSize) {
                            result.add(video);
                            existingIds.add(video.videoId());
                        }
                    }
                    log.debug("3차 카테고리 데이터: {}개", result.size());
                } catch (Exception e) {
                    log.warn("같은 카테고리 데이터 추가 실패", e);
                }
            }
            
            log.info("확장 수집 완료: {}개", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("확장된 데이터 수집 실패 - fallback to basic", e);
            return fetchYoutubeTop100(region, categoryId);
        }
    }
    

    private void validateParameters(String region, String categoryId, int size) {
        if (region != null && region.length() != 2) {
            throw new TubetenException(ErrorCode.INVALID_REGION_CODE, 
                    "지역 코드는 2자리여야 합니다: " + region);
        }
        
        if (categoryId != null && !categoryId.isBlank() && !categoryId.matches("\\d+")) {
            throw new TubetenException(ErrorCode.INVALID_CATEGORY_ID, 
                    "카테고리 ID는 숫자여야 합니다: " + categoryId);
        }
        
        if (size <= 0 || size > 200) {
            throw new TubetenException(ErrorCode.INVALID_PARAMETER, 
                    "size는 1-200 사이의 값이어야 합니다: " + size);
        }
    }
}
