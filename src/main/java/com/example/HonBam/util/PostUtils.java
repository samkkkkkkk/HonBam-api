package com.example.HonBam.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.swing.text.html.Option;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostUtils {
    private final ObjectMapper objectMapper;

    /**
     * List<String> 형태의 이미지 URL 목록을 널 체크 후 안전하게 JSON 문자열로 변환합니다.
     *
     * @param imageUrls List<String> 이미지 URL 목록 (null일 수 있음)
     * @return JSON 문자열 (예: "[\"a.jpg\",\"b.jpg\"]" 또는 "[]")
     * @throws RuntimeException JSON 변환 실패 시 발생
     */
    public String safeConvertImageUrlsToJson(List<String> imageUrls) {
        // null인 경우 Collections.empty()로 안전하게 대체
        List<String> safeList = Optional.ofNullable(imageUrls)
                .orElseGet(Collections::emptyList);
        try {
            // ObjectMapper를 사용하여 JSON 문자열로 변환
            return objectMapper.writeValueAsString(safeList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이미지 URL 목록을 JSON으로 변환하는 중 오류가 발생했습니다.");
        }
    }

}
