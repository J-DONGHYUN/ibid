package project.kjhjdh.ibid.support.seed;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seed")
public record SeedProperties(
        boolean enabled,
        Integer users,
        Integer tags,
        Integer products,
        Integer productImages,
        Integer productTags,
        Integer productLikes,
        Integer orders,
        Integer batchSize,
        Integer jpaBaselineRows,
        Boolean truncateBeforeSeed,
        Boolean disableIntegrityChecksWhileSeeding,
        Boolean recreateLikeIndexAfterSeed,
        Long randomSeed
) {

    private static final int DEFAULT_USERS = 10_000;
    private static final int DEFAULT_TAGS = 200;
    private static final int DEFAULT_PRODUCTS = 2_000_000;
    private static final int DEFAULT_PRODUCT_IMAGES = 3_000_000;
    private static final int DEFAULT_PRODUCT_TAGS = 4_000_000;
    private static final int DEFAULT_PRODUCT_LIKES = 1_000_000;
    private static final int DEFAULT_ORDERS = 300_000;
    private static final int DEFAULT_BATCH_SIZE = 1_000;
    private static final int DEFAULT_JPA_BASELINE_ROWS = 20_000;
    private static final long DEFAULT_RANDOM_SEED = 20260811L;

    public int userCount() {
        return orDefault(users, DEFAULT_USERS);
    }

    public int tagCount() {
        return orDefault(tags, DEFAULT_TAGS);
    }

    public int productCount() {
        return orDefault(products, DEFAULT_PRODUCTS);
    }

    public int productImageCount() {
        return orDefault(productImages, DEFAULT_PRODUCT_IMAGES);
    }

    public int productTagCount() {
        return orDefault(productTags, DEFAULT_PRODUCT_TAGS);
    }

    public int productLikeCount() {
        return orDefault(productLikes, DEFAULT_PRODUCT_LIKES);
    }

    public int orderCount() {
        return orDefault(orders, DEFAULT_ORDERS);
    }

    public int batch() {
        return orDefault(batchSize, DEFAULT_BATCH_SIZE);
    }

    public int jpaBaseline() {
        return orDefault(jpaBaselineRows, DEFAULT_JPA_BASELINE_ROWS);
    }

    public boolean truncateFirst() {
        return truncateBeforeSeed == null || truncateBeforeSeed;
    }

    public boolean relaxIntegrityChecks() {
        return disableIntegrityChecksWhileSeeding == null || disableIntegrityChecksWhileSeeding;
    }

    public boolean recreateLikeIndex() {
        return Boolean.TRUE.equals(recreateLikeIndexAfterSeed);
    }

    public long seed() {
        return randomSeed == null ? DEFAULT_RANDOM_SEED : randomSeed;
    }

    private static int orDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
