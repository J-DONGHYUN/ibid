package project.kjhjdh.ibid.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthRedisTimeoutLoadTest extends AuthRedisFaultSupport {

    private static final int REQUESTS = 300;
    private static final int THREADS = 16;

    @DisplayName("[AU-02] 정상 Redis 에서 동시 로그인이 몰려도 1초 타임아웃에 걸려 실패하는 요청이 없다 (T-50 오탐 측정)")
    @Test
    void login_underConcurrentLoad_hasNoTimeoutFailures() throws Exception {
        Account account = signupAndLogin();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Measured>> futures = new ArrayList<>();
        for (int i = 0; i < REQUESTS; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                return login(account.email());
            }));
        }

        start.countDown();
        List<Measured> results = new ArrayList<>();
        for (Future<Measured> future : futures) {
            results.add(future.get(120, TimeUnit.SECONDS));
        }
        executor.shutdown();

        long failures = results.stream().filter(r -> r.status() != 200).count();
        long maxMillis = results.stream().mapToLong(r -> r.elapsed().toMillis()).max().orElse(0);
        assertThat(failures).as("동시 로그인 %d 건 (스레드 %d) 중 실패 수 · 최대 응답 %dms", REQUESTS, THREADS, maxMillis).isZero();
    }
}
