package project.kjhjdh.ibid.product.infra;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductViewRedisRepository {

    private static final String DEDUP_KEY_PREFIX = "product-view::%s::%s";
    private static final String COUNT_KEY_PREFIX = "product-view-count::%s";
    private static final String DIRTY_KEY = "product-view-dirty";

    private static final Long RECORDED = 1L;

    private static final RedisScript<Long> RECORD_VIEW_SCRIPT =
            RedisScript.of(new ClassPathResource("scripts/record-product-view.lua"), Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    public boolean recordView(Long productId, String visitorId, Duration dedupTtl) {
        try {
            Long result = stringRedisTemplate.execute(
                    RECORD_VIEW_SCRIPT,
                    List.of(generateDedupKey(productId, visitorId), generateCountKey(productId), DIRTY_KEY),
                    String.valueOf(dedupTtl.toMillis()),
                    String.valueOf(productId)
            );
            return RECORDED.equals(result);
        } catch (DataAccessException e) {
            log.warn("조회수 기록 실패 productId={} visitorId={}", productId, visitorId, e);
            return false;
        }
    }

    public long findPendingCount(Long productId) {
        try {
            return toCount(stringRedisTemplate.opsForValue().get(generateCountKey(productId)));
        } catch (DataAccessException e) {
            log.warn("미반영 조회수 조회 실패 productId={}", productId, e);
            return 0L;
        }
    }

    public Set<Long> findPendingProductIds() {
        Set<String> members = stringRedisTemplate.opsForSet().members(DIRTY_KEY);
        if (members == null) {
            return Set.of();
        }
        return members.stream()
                .map(Long::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    public long takePendingCount(Long productId) {
        stringRedisTemplate.opsForSet().remove(DIRTY_KEY, String.valueOf(productId));
        return toCount(stringRedisTemplate.opsForValue().getAndDelete(generateCountKey(productId)));
    }

    private long toCount(String value) {
        return value == null ? 0L : Long.parseLong(value);
    }

    private String generateDedupKey(Long productId, String visitorId) {
        return DEDUP_KEY_PREFIX.formatted(String.valueOf(productId), visitorId);
    }

    private String generateCountKey(Long productId) {
        return COUNT_KEY_PREFIX.formatted(String.valueOf(productId));
    }
}
