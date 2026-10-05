package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import project.kjhjdh.ibid.support.IntegrationTestSupport;

class RedisTimeoutConfigTest extends IntegrationTestSupport {

    private static final Duration ACCEPTABLE_COMMAND_TIMEOUT = Duration.ofSeconds(3);

    @Autowired
    private LettuceConnectionFactory lettuceConnectionFactory;

    @DisplayName("[PD-09] Redis 명령 타임아웃이 짧게 설정돼 있어 무응답이 상세 응답을 오래 붙잡지 않는다 (QA-B.1)")
    @Test
    @Disabled("QA-B.1 발견 — Redis 명령 타임아웃이 기본 60초다(설정 없음). 무응답이면 상세 한 건이 120초 걸렸다(실측). 타임아웃을 설정하는 티켓에서 지운다")
    void redisCommandTimeoutIsBounded() {
        // when
        Duration commandTimeout = lettuceConnectionFactory.getClientConfiguration().getCommandTimeout();

        // then — 상세 한 건이 Redis 를 두 번 부른다(기록 · 합산). 타임아웃이 60초면 응답 하나가 2분 걸린다
        assertThat(commandTimeout).as("Redis 명령 타임아웃").isLessThanOrEqualTo(ACCEPTABLE_COMMAND_TIMEOUT);
    }
}
