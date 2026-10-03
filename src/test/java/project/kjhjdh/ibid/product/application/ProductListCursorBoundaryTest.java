package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class ProductListCursorBoundaryTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("[PD-03] 커서가 음수면 빈 목록이 돌아온다 (QA-3.3)")
    @Test
    void getProducts_negativeCursor() {
        // given
        saveProducts(3);

        // when — 클라이언트가 음수 커서를 보낸다
        ProductListResult result = productService.getProducts(-1L, true);

        // then — id < -1 이라 비어 있다. 예외로 깨지지 않는다
        assertThat(result.slice().getContent()).isEmpty();
        assertThat(result.slice().hasNext()).isFalse();
    }

    @DisplayName("[PD-03] 커서가 Long.MAX_VALUE 면 첫 페이지와 같다 (QA-3.3)")
    @Test
    void getProducts_maxCursor() {
        // given
        saveProducts(3);

        // when — 커서를 비운 첫 요청과 Long.MAX_VALUE 를 비교한다
        List<Long> withMax = idsOf(productService.getProducts(Long.MAX_VALUE, true));
        List<Long> withNull = idsOf(productService.getProducts(null, true));

        // then — 커서 없음은 Long.MAX_VALUE 로 치환되므로 같은 결과다
        assertThat(withMax).isEqualTo(withNull).hasSize(3);
    }

    @DisplayName("[PD-03] 커서가 0 이면 빈 목록이 돌아온다 (QA-3.3)")
    @Test
    void getProducts_zeroCursor() {
        // given
        saveProducts(3);

        // when
        ProductListResult result = productService.getProducts(0L, true);

        // then — id 는 1 부터라 비어 있다
        assertThat(result.slice().getContent()).isEmpty();
    }

    @DisplayName("[PD-03] 마지막 id 를 커서로 주면 그보다 작은 것만 돌아온다 (QA-3.3)")
    @Test
    void getProducts_cursorIsExclusive() {
        // given
        List<Long> ids = saveProducts(3);
        Long last = ids.get(ids.size() - 1);

        // when
        List<Long> page = idsOf(productService.getProducts(last, true));

        // then — 커서 자신은 빠진다 (idLessThan)
        assertThat(page).doesNotContain(last).hasSize(2);
    }

    private List<Long> idsOf(ProductListResult result) {
        return result.slice().getContent().stream().map(Product::getId).toList();
    }

    private List<Long> saveProducts(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(productRepository.save(Product.create(
                    SELLER_ID, "아이폰 " + i, "A급", 500000,
                    ProductCondition.USED, DeviceSpecFixture.sample())).getId());
        }
        return ids;
    }
}
