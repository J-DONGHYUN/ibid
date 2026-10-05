package project.kjhjdh.ibid.auth.presentation;

import static project.kjhjdh.ibid.auth.presentation.cookie.RefreshTokenCookieHandler.REFRESH_TOKEN_COOKIE_NAME;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import project.kjhjdh.ibid.support.WebIntegrationTestSupport;

abstract class AuthRedisFaultSupport extends WebIntegrationTestSupport {

    protected static final String PASSWORD = "pass1234";

    protected record Measured(int status, Duration elapsed, List<String> setCookies) {

        boolean clearsRefreshCookie() {
            return setCookies.stream().anyMatch(c -> c.startsWith(REFRESH_TOKEN_COOKIE_NAME + "=;") && c.contains("Max-Age=0"));
        }

        String summary() {
            return "status=%d elapsed=%dms clearsRefreshCookie=%s".formatted(status, elapsed.toMillis(), clearsRefreshCookie());
        }
    }

    protected record Account(String email, String refreshCookie) {
    }

    protected Account signupAndLogin() {
        String email = "u" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String username = "u" + UUID.randomUUID().toString().substring(0, 5);
        restTemplate.postForEntity("/api/auth/signup", Map.of("email", email, "password", PASSWORD, "username", username), String.class);
        ResponseEntity<String> login = restTemplate.postForEntity("/api/auth/login", Map.of("email", email, "password", PASSWORD), String.class);
        String setCookie = login.getHeaders().get(HttpHeaders.SET_COOKIE).get(0);
        String refreshCookie = setCookie.substring(0, setCookie.indexOf(';'));
        return new Account(email, refreshCookie);
    }

    protected Measured login(String email) {
        return post("/api/auth/login", Map.of("email", email, "password", PASSWORD), null);
    }

    protected Measured refresh(String refreshCookie) {
        return post("/api/auth/refresh", null, refreshCookie);
    }

    protected Measured logout(String refreshCookie) {
        return post("/api/auth/logout", null, refreshCookie);
    }

    private Measured post(String path, Object body, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        Instant startedAt = Instant.now();
        ResponseEntity<String> response = restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        Duration elapsed = Duration.between(startedAt, Instant.now());
        List<String> setCookies = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE);
        return new Measured(response.getStatusCode().value(), elapsed, setCookies);
    }
}
