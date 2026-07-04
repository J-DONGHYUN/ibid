package project.kjhjdh.ibid.product.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.support.RepositoryTestSupport;

class ProductRepositoryTest extends RepositoryTestSupport {

    private static final Long SELLER_ID = 1L;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("상품을 최신 등록순으로 조회한다")
    @Test
    void findAllByOrderByIdDesc() {
        // given
        Product first = productRepository.save(Product.create(SELLER_ID, "첫번째", "상태 좋음", 10000, 1));
        Product second = productRepository.save(Product.create(SELLER_ID, "두번째", "상태 좋음", 20000, 2));

        // when
        List<Product> products = productRepository.findAllByOrderByIdDesc();

        // then
        assertThat(products).extracting(Product::getId)
                .containsExactly(second.getId(), first.getId());
    }
}
