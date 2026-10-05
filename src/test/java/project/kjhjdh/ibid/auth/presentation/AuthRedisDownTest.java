package project.kjhjdh.ibid.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthRedisDownTest extends AuthRedisFaultSupport {

    private static final Duration ACCEPTABLE_LATENCY = Duration.ofSeconds(5);

    @Autowired
    @Qualifier("redisContainer")
    private GenericContainer<?> redisContainer;

    @DisplayName("[AU-02] Redis 연결이 거부될 때 로그인 · 재발급(AU-03) · 로그아웃(AU-04) 의 응답 (T-50 측정)")
    @Test
    void auth_whileRedisIsDown() {
        Account account = signupAndLogin();
        redisContainer.stop();

        Measured login = login(account.email());
        Measured refresh = refresh(account.refreshCookie());
        Measured logout = logout(account.refreshCookie());

        String all = "login: %s | refresh: %s | logout: %s".formatted(login.summary(), refresh.summary(), logout.summary());
        assertThat(List.of(login.status(), refresh.status(), logout.status())).as("연결 거부 중 상태 코드 — %s", all).containsOnly(500);
        assertThat(List.of(login.elapsed(), refresh.elapsed(), logout.elapsed()))
                .as("연결 거부 중 응답 시간 — %s", all).allSatisfy(elapsed -> assertThat(elapsed).isLessThan(ACCEPTABLE_LATENCY));
        assertThat(List.of(refresh.clearsRefreshCookie(), logout.clearsRefreshCookie())).as("refresh 쿠키를 지우는가 — %s", all).containsOnly(false);
    }
}
