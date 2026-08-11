package project.kjhjdh.ibid.support.seed;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.security.crypto.password.PasswordEncoder;

import project.kjhjdh.ibid.order.domain.OrderStatus;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.user.domain.Role;

public class DataSeeder implements ApplicationRunner, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String INSERT_USER =
            "INSERT INTO users (id, email, password, username, role) VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_TAG =
            "INSERT INTO tags (id, name) VALUES (?, ?)";
    private static final String INSERT_PRODUCT = """
            INSERT INTO products
                (id, seller_id, title, description, price, stock, status, view_count,
                 product_condition, shipping_fee, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String INSERT_PRODUCT_IMAGE =
            "INSERT INTO product_images (id, product_id, url, extension, sort_order) VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_PRODUCT_TAG =
            "INSERT INTO product_tag (product_id, tag_id) VALUES (?, ?)";
    private static final String INSERT_PRODUCT_LIKE =
            "INSERT INTO product_likes (id, user_id, product_id, created_at) VALUES (?, ?, ?, ?)";
    private static final String INSERT_ORDER = """
            INSERT INTO orders (id, product_id, buyer_id, seller_id, quantity, total_price, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String LIKE_INDEX_NAME = "idx_product_likes_product";
    private static final String SEEDED_PASSWORD = "pass1234";

    private final DataSource dataSource;
    private final SeedProperties properties;
    private final ProductJpaBaseline jpaBaseline;
    private final PasswordEncoder passwordEncoder;
    private final List<PhaseResult> results = new ArrayList<>();

    public DataSeeder(DataSource dataSource, SeedProperties properties,
                      ProductJpaBaseline jpaBaseline, PasswordEncoder passwordEncoder) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.jpaBaseline = jpaBaseline;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            BulkInserter inserter = new BulkInserter(connection, properties.batch());

            logConfiguration(connection);

            if (properties.truncateFirst()) {
                truncateAll(connection);
            }
            if (properties.relaxIntegrityChecks()) {
                inserter.execute("SET FOREIGN_KEY_CHECKS = 0");
            }

            runJpaBaseline(connection);
            seed(inserter);

            if (properties.relaxIntegrityChecks()) {
                inserter.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            logSummary(inserter);
        }
    }

    private void seed(BulkInserter inserter) throws SQLException {
        SeedRandomData data = new SeedRandomData(
                properties.seed(), properties.userCount(), properties.tagCount(),
                properties.productCount(), LocalDateTime.now()
        );

        seedUsers(inserter, data);
        seedTags(inserter, data);
        seedProducts(inserter, data);
        seedProductImages(inserter, data);
        seedProductTags(inserter, data);
        seedProductLikes(inserter, data);
        seedOrders(inserter, data);
    }

    private void seedUsers(BulkInserter inserter, SeedRandomData data) throws SQLException {
        String encodedPassword = passwordEncoder.encode(SEEDED_PASSWORD);
        int total = properties.userCount();
        long elapsed = inserter.insert("users", INSERT_USER, total, (statement, rowIndex) -> {
            statement.setLong(1, rowIndex + 1L);
            statement.setString(2, data.userEmail(rowIndex));
            statement.setString(3, encodedPassword);
            statement.setString(4, data.username(rowIndex));
            statement.setString(5, Role.USER.name());
        });
        record("users", total, elapsed);
    }

    private void seedTags(BulkInserter inserter, SeedRandomData data) throws SQLException {
        int total = properties.tagCount();
        long elapsed = inserter.insert("tags", INSERT_TAG, total, (statement, rowIndex) -> {
            statement.setLong(1, rowIndex + 1L);
            statement.setString(2, data.tagName(rowIndex));
        });
        record("tags", total, elapsed);
    }

    private void seedProducts(BulkInserter inserter, SeedRandomData data) throws SQLException {
        int total = properties.productCount();
        long elapsed = inserter.insert("products", INSERT_PRODUCT, total, (statement, rowIndex) -> {
            String title = data.title(rowIndex);
            ProductStatus status = data.productStatus();
            statement.setLong(1, rowIndex + 1L);
            statement.setLong(2, data.sellerIdFor(rowIndex));
            statement.setString(3, title);
            statement.setString(4, data.description(title));
            statement.setInt(5, data.price(rowIndex));
            statement.setInt(6, data.stock(status));
            statement.setString(7, status.name());
            statement.setLong(8, data.viewCount());
            statement.setString(9, data.productCondition().name());
            statement.setInt(10, data.shippingFee());
            statement.setTimestamp(11, Timestamp.valueOf(data.createdAt()));
        });
        record("products (jdbc batch)", total, elapsed);
    }

    private void seedProductImages(BulkInserter inserter, SeedRandomData data) throws SQLException {
        int total = properties.productImageCount();
        long elapsed = inserter.insert("product_images", INSERT_PRODUCT_IMAGE, total, (statement, rowIndex) -> {
            int productId = data.uniformProductIndex() + 1;
            int sortOrder = data.imageSortOrder();
            statement.setLong(1, rowIndex + 1L);
            statement.setLong(2, productId);
            statement.setString(3, data.imageUrl(productId, sortOrder));
            statement.setString(4, "JPG");
            statement.setInt(5, sortOrder);
        });
        record("product_images", total, elapsed);
    }

    private void seedProductTags(BulkInserter inserter, SeedRandomData data) throws SQLException {
        int total = properties.productTagCount();
        int productCount = properties.productCount();
        int[] cursor = {0, 0};
        long elapsed = inserter.insert("product_tag", INSERT_PRODUCT_TAG, total, (statement, rowIndex) -> {
            if (cursor[1] == 0) {
                cursor[1] = Math.max(1, data.tagsPerProduct());
            }
            int productIndex = cursor[0] % productCount;
            int slot = cursor[1] - 1;
            statement.setLong(1, productIndex + 1L);
            statement.setLong(2, data.tagIdFor(productIndex, slot));
            cursor[1]--;
            if (cursor[1] == 0) {
                cursor[0]++;
            }
        });
        record("product_tag", total, elapsed);
    }

    private void seedProductLikes(BulkInserter inserter, SeedRandomData data) throws SQLException {
        boolean recreateIndex = properties.recreateLikeIndex();
        if (recreateIndex) {
            inserter.execute("DROP INDEX " + LIKE_INDEX_NAME + " ON product_likes");
        }

        int total = properties.productLikeCount();
        int userCount = properties.userCount();
        int[] likesPerProduct = new int[properties.productCount()];
        int[] fallbackCursor = {0};
        LocalDateTime likeBase = LocalDateTime.now();

        long elapsed = inserter.insert("product_likes", INSERT_PRODUCT_LIKE, total, (statement, rowIndex) -> {
            int productIndex = data.popularProductIndex();
            if (likesPerProduct[productIndex] >= userCount) {
                productIndex = nextAvailable(likesPerProduct, userCount, fallbackCursor);
            }
            int userId = ++likesPerProduct[productIndex];
            statement.setLong(1, rowIndex + 1L);
            statement.setLong(2, userId);
            statement.setLong(3, productIndex + 1L);
            statement.setTimestamp(4, Timestamp.valueOf(likeBase.minusSeconds(rowIndex % 31_536_000)));
        });
        record("product_likes", total, elapsed);

        if (recreateIndex) {
            long indexElapsed = System.nanoTime();
            inserter.execute("CREATE INDEX " + LIKE_INDEX_NAME + " ON product_likes (product_id)");
            record("create " + LIKE_INDEX_NAME, 0, (System.nanoTime() - indexElapsed) / 1_000_000L);
        }
    }

    private void seedOrders(BulkInserter inserter, SeedRandomData data) throws SQLException {
        int total = properties.orderCount();
        long elapsed = inserter.insert("orders", INSERT_ORDER, total, (statement, rowIndex) -> {
            int productIndex = data.uniformProductIndex();
            long sellerId = data.sellerOf(productIndex);
            int quantity = data.quantity();
            OrderStatus status = data.orderStatus();
            statement.setLong(1, rowIndex + 1L);
            statement.setLong(2, productIndex + 1L);
            statement.setLong(3, data.buyerIdOtherThan(sellerId));
            statement.setLong(4, sellerId);
            statement.setInt(5, quantity);
            statement.setInt(6, data.priceOf(productIndex) * quantity);
            statement.setString(7, status.name());
        });
        record("orders", total, elapsed);
    }

    private int nextAvailable(int[] likesPerProduct, int userCount, int[] cursor) {
        for (int scanned = 0; scanned < likesPerProduct.length; scanned++) {
            int candidate = cursor[0];
            cursor[0] = (cursor[0] + 1) % likesPerProduct.length;
            if (likesPerProduct[candidate] < userCount) {
                return candidate;
            }
        }
        throw new IllegalStateException("찜을 더 넣을 수 있는 상품이 없습니다. seed.product-likes를 줄이거나 seed.users를 늘리세요.");
    }

    private void runJpaBaseline(Connection connection) throws SQLException {
        int total = properties.jpaBaseline();
        if (total <= 0) {
            return;
        }
        long elapsed = jpaBaseline.insert(total, properties.batch());
        record("products (jpa saveAll, 기준선)", total, elapsed);
        try (Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE products");
        }
        connection.commit();
    }

    private void truncateAll(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            List<String> tables = new ArrayList<>();
            try (var resultSet = statement.executeQuery("""
                    SELECT table_name FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'
                    """)) {
                while (resultSet.next()) {
                    tables.add(resultSet.getString(1));
                }
            }
            for (String table : tables) {
                statement.execute("TRUNCATE TABLE `" + table + "`");
            }
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
        connection.commit();
    }

    private void logConfiguration(Connection connection) throws SQLException {
        log.info("=== 시딩 시작 ===");
        log.info("  jdbcUrl                = {}", connection.getMetaData().getURL());
        log.info("  batchSize              = {}", properties.batch());
        log.info("  products               = {}", properties.productCount());
        log.info("  fkChecksRelaxed        = {}", properties.relaxIntegrityChecks());
        log.info("  recreateLikeIndex      = {}", properties.recreateLikeIndex());
        log.info("  randomSeed             = {}", properties.seed());
    }

    private void logSummary(BulkInserter inserter) throws SQLException {
        log.info("=== 시딩 결과 ===");
        log.info("{}", "-".repeat(72));
        log.info(String.format("%-34s %12s %10s %12s", "phase", "rows", "elapsed", "rows/sec"));
        log.info("{}", "-".repeat(72));
        long totalMillis = 0;
        for (PhaseResult result : results) {
            totalMillis += result.elapsedMillis();
            log.info(String.format("%-34s %12d %9.1fs %12s",
                    result.label(), result.rows(), result.elapsedMillis() / 1000.0, result.rowsPerSecond()));
        }
        log.info("{}", "-".repeat(72));
        log.info(String.format("%-34s %12s %9.1fs", "합계", "", totalMillis / 1000.0));
        log.info("=== 테이블 행수 ===");
        for (String table : List.of("users", "tags", "products", "product_images", "product_tag",
                "product_likes", "orders")) {
            log.info("  {} = {}", table, inserter.countOf(table));
        }
    }

    private void record(String label, int rows, long elapsedMillis) {
        results.add(new PhaseResult(label, rows, elapsedMillis));
    }

    private record PhaseResult(String label, int rows, long elapsedMillis) {

        String rowsPerSecond() {
            if (rows == 0 || elapsedMillis == 0) {
                return "-";
            }
            return String.valueOf(rows * 1000L / elapsedMillis);
        }
    }
}
