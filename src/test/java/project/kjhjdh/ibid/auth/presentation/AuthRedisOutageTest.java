package project.kjhjdh.ibid.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthRedisOutageTest extends AuthRedisFaultSupport {

    private static final Duration ACCEPTABLE_LATENCY = Duration.ofSeconds(5);

    @Autowired
    @Qualifier("redisContainer")
    private GenericContainer<?> redisContainer;

    private boolean paused;

    @AfterEach
    void unpauseRedis() {
        if (paused) {
            resume();
        }
    }

    @DisplayName("[AU-02] Redis 가 응답하지 않을 때 로그인의 응답 (T-50 측정)")
    @Test
    void login_whileRedisIsUnresponsive() {
        Account account = signupAndLogin();
        pause();

        Measured result = login(account.email());

        assertThat(result.status()).as("무응답 중 로그인 — %s", result.summary()).isEqualTo(500);
        assertThat(result.elapsed()).as("무응답 중 로그인 응답 시간 (%s)", result.summary()).isLessThan(ACCEPTABLE_LATENCY);
    }

    @DisplayName("[AU-03] Redis 가 응답하지 않을 때 토큰 재발급의 응답 (T-50 측정)")
    @Test
    void refresh_whileRedisIsUnresponsive() {
        Account account = signupAndLogin();
        pause();

        Measured result = refresh(account.refreshCookie());

        assertThat(result.status()).as("무응답 중 재발급 — %s", result.summary()).isEqualTo(500);
        assertThat(result.elapsed()).as("무응답 중 재발급 응답 시간 (%s)", result.summary()).isLessThan(ACCEPTABLE_LATENCY);
        assertThat(result.clearsRefreshCookie()).as("무응답 중 재발급이 refresh 쿠키를 지우는가 — %s", result.summary()).isFalse();
    }

    @DisplayName("[AU-04] Redis 가 응답하지 않을 때 로그아웃의 응답 (T-50 측정)")
    @Test
    void logout_whileRedisIsUnresponsive() {
        Account account = signupAndLogin();
        pause();

        Measured result = logout(account.refreshCookie());

        assertThat(result.status()).as("무응답 중 로그아웃 — %s", result.summary()).isEqualTo(500);
        assertThat(result.elapsed()).as("무응답 중 로그아웃 응답 시간 (%s)", result.summary()).isLessThan(ACCEPTABLE_LATENCY);
        assertThat(result.clearsRefreshCookie()).as("무응답 중 로그아웃이 refresh 쿠키를 지우는가 — %s", result.summary()).isFalse();
    }

    @DisplayName("[AU-02] Redis 가 되살아나면 로그인 · 재발급 · 로그아웃이 정상으로 돌아온다 (T-50 복구 대조)")
    @Test
    void auth_recoversAfterRedisResumes() {
        Account account = signupAndLogin();
        pause();
        login(account.email());
        resume();

        Measured login = login(account.email());
        Measured refresh = refresh(account.refreshCookie());

        assertThat(login.status()).as("복구 뒤 로그인 %s", login.summary()).isEqualTo(200);
        assertThat(refresh.status()).as("복구 뒤 재발급 %s", refresh.summary()).isIn(200, 401);
    }

    private void pause() {
        redisContainer.getDockerClient().pauseContainerCmd(redisContainer.getContainerId()).exec();
        paused = true;
    }

    private void resume() {
        redisContainer.getDockerClient().unpauseContainerCmd(redisContainer.getContainerId()).exec();
        paused = false;
    }
}
