package com.example.HonBam.filter;

import com.example.HonBam.exception.JwtAuthException;
import com.example.HonBam.exception.JwtErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtExceptionFilter 단위 테스트.
 * 체인에서 발생한 JwtAuthException이 사유 코드에 맞는 상태/바디로 매핑되는지 검증한다(F-1/F-1b 통합 가드).
 */
class JwtExceptionFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JwtExceptionFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtExceptionFilter(objectMapper);
    }

    private JsonNode runWith(JwtException toThrow, MockHttpServletResponse response) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        FilterChain chain = (req, res) -> {
            throw toThrow;
        };
        filter.doFilter(request, response, chain);
        return objectMapper.readTree(response.getContentAsString());
    }

    @Test
    @DisplayName("ACCESS_TOKEN_EXPIRED → 401 + {message, code}")
    void mapsAccessTokenExpired() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        JsonNode body = runWith(new JwtAuthException(JwtErrorCode.ACCESS_TOKEN_EXPIRED), response);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("message").asText()).isEqualTo("ACCESS_TOKEN_EXPIRED");
        assertThat(body.get("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("INVALID_TOKEN_TYPE → 400 + {message, code}")
    void mapsInvalidTokenType() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        JsonNode body = runWith(new JwtAuthException(JwtErrorCode.INVALID_TOKEN_TYPE), response);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(body.get("message").asText()).isEqualTo("INVALID_TOKEN_TYPE");
        assertThat(body.get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("타입 없는 일반 JwtException → fallback INVALID_JWT(401)")
    void fallsBackToInvalidJwt() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        JsonNode body = runWith(new JwtException("something else"), response);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("message").asText()).isEqualTo("INVALID_JWT");
        assertThat(body.get("code").asInt()).isEqualTo(401);
    }
}
