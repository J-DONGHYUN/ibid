package project.kjhjdh.ibid.support.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import project.kjhjdh.ibid.TestcontainersConfiguration;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "seed.enabled=true",
                "seed.users=50",
                "seed.tags=20",
                "seed.products=1000",
                "seed.product-images=1500",
                "seed.product-tags=2000",
                "seed.product-likes=800",
                "seed.orders=200",
                "seed.jpa-baseline-rows=100"
        }
)
@Import(TestcontainersConfiguration.class)
class DataSeederTest {

    private static boolean seeded;

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void resetSeededFlag() {
        seeded = false;
    }

    @DisplayName("시딩하면 설정한 행수만큼 모든 테이블에 데이터가 생성된다")
    @Test
    void seed_fillsEveryTable() {
        // given
        seedOnce();

        // when & then
        assertThat(count("users")).isEqualTo(50);
        assertThat(count("tags")).isEqualTo(20);
        assertThat(count("products")).isEqualTo(1000);
        assertThat(count("product_images")).isEqualTo(1500);
        assertThat(count("product_tag")).isEqualTo(2000);
        assertThat(count("product_likes")).isEqualTo(800);
        assertThat(count("orders")).isEqualTo(200);
    }

    @DisplayName("찜은 같은 사용자·상품 조합이 중복되지 않는다")
    @Test
    void seed_keepsProductLikePairsUnique() {
        // given
        seedOnce();

        // when
        Long distinctPairs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM (SELECT user_id, product_id FROM product_likes GROUP BY user_id, product_id) p",
                Long.class
        );

        // then
        assertThat(distinctPairs).isEqualTo(count("product_likes"));
    }

    @DisplayName("판매중인 상품은 재고가 남아 있고 품절 상품은 재고가 0이다")
    @Test
    void seed_keepsStockConsistentWithStatus() {
        // given
        seedOnce();

        // when
        Long onSaleWithoutStock = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE status = 'ON_SALE' AND stock <= 0", Long.class);
        Long soldOutWithStock = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE status = 'SOLD_OUT' AND stock <> 0", Long.class);

        // then
        assertThat(onSaleWithoutStock).isZero();
        assertThat(soldOutWithStock).isZero();
    }

    @DisplayName("상품은 도메인 제약(제목 100자·설명 2000자·가격 1원 이상)을 지킨다")
    @Test
    void seed_respectsProductInvariants() {
        // given
        seedOnce();

        // when
        Long violations = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM products
                WHERE CHAR_LENGTH(title) > 100
                   OR CHAR_LENGTH(description) > 2000
                   OR price < 1
                   OR shipping_fee < 0
                """, Long.class);

        // then
        assertThat(violations).isZero();
    }

    @DisplayName("주문의 판매자는 해당 상품의 실제 판매자와 일치한다")
    @Test
    void seed_keepsOrderSellerMatchedWithProduct() {
        // given
        seedOnce();

        // when
        Long mismatched = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM orders o
                JOIN products p ON p.id = o.product_id
                WHERE o.seller_id <> p.seller_id
                """, Long.class);

        // then
        assertThat(mismatched).isZero();
    }

    @DisplayName("상품 등록시각은 한 시점에 몰리지 않고 기간에 분산된다")
    @Test
    void seed_spreadsProductCreatedAt() {
        // given
        seedOnce();

        // when
        Long distinctDays = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT DATE(created_at)) FROM products", Long.class);

        // then
        assertThat(distinctDays).isGreaterThan(30L);
    }

    private synchronized void seedOnce() {
        if (seeded) {
            return;
        }
        try {
            dataSeeder.run(new DefaultApplicationArguments());
        } catch (Exception e) {
            throw new IllegalStateException("시딩 실패", e);
        }
        seeded = true;
    }

    private long count(String table) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + table + "`", Long.class);
        return count == null ? 0L : count;
    }
}
