package project.kjhjdh.ibid.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import project.kjhjdh.ibid.support.WebIntegrationTestSupport;

class ProtectedEndpointAuthTest extends WebIntegrationTestSupport {

    @DisplayName("[AU-02] 토큰 없이 보호 API 를 부르면 401 이다 (QA-3.1)")
    @ParameterizedTest
    @CsvSource({
            "GET,    /api/notifications",
            "POST,   /api/notifications/1/read",
            "POST,   /api/products/1/reservation",
            "DELETE, /api/products/1/reservation",
            "POST,   /api/products/1/completion",
            "GET,    /api/products/me/sales",
            "GET,    /api/products/me/purchases",
            "POST,   /api/products",
            "PATCH,  /api/products/1",
            "DELETE, /api/products/1",
            "GET,    /api/chat-rooms",
            "POST,   /api/chat-rooms/1/messages",
            "GET,    /api/chat-rooms/1/messages",
            "POST,   /api/chat-rooms/1/read",
            "POST,   /api/products/1/chat-rooms",
            "GET,    /api/products/1/chat-rooms",
    })
    void protectedEndpoint_withoutToken(String method, String path) {
        // when — 토큰 없이 호출한다
        ResponseEntity<String> response = restTemplate.exchange(
                path, HttpMethod.valueOf(method), HttpEntity.EMPTY, String.class);

        // then — 인터셉터가 핸들러 앞에서 막는다. 단위 테스트는 인터셉터의 판정만 보고
        //        이 경로에 실제로 등록됐는지는 증명하지 못한다
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("[AU-02] 공개 API 는 토큰 없이도 401 이 아니다 (QA-3.1 대조군)")
    @ParameterizedTest
    @CsvSource({
            "GET, /api/products",
            "GET, /api/products/1",
    })
    void publicEndpoint_withoutToken(String method, String path) {
        // when
        ResponseEntity<String> response = restTemplate.exchange(
                path, HttpMethod.valueOf(method), HttpEntity.EMPTY, String.class);

        // then — @PublicApi 가 붙은 목록 · 상세는 열려 있다 (없는 상품은 404)
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
