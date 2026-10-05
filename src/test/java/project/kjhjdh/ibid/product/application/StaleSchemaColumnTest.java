package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import project.kjhjdh.ibid.product.domain.DeviceCategory;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StaleSchemaColumnTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("[PD-01] 필드를 지운 뒤 DB 에 남은 NOT NULL 컬럼이 상품 등록을 깨뜨린다 (QA-C.2 — 알려진 함정)")
    @Test
    void register_failsOnLeftoverNotNullColumn() {
        // given — 과거 필드의 컬럼이 NOT NULL 로 남은 운영 DB 를 흉내 낸다. ddl-auto: update 는 컬럼을 지우지 않는다
        jdbcTemplate.execute("ALTER TABLE products ADD COLUMN legacy_stock INT NOT NULL");
        ProductRegisterCommand command = new ProductRegisterCommand("아이폰 13", "A급", 500000, ProductCondition.USED,
                new DeviceSpecCommand(DeviceCategory.SMARTPHONE, "iPhone 13", 90, "본체", null));

        // when & then — 코드는 그 컬럼을 모르므로 INSERT 가 실패한다. 매번 새 컨테이너를 쓰는 테스트는 이 함정을 못 본다
        assertThatThrownBy(() -> productService.register(SELLER_ID, command))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("legacy_stock");
        assertThat(productRepository.count()).isZero();
    }
}
