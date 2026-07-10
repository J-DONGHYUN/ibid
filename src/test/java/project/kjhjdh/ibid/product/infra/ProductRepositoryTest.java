package project.kjhjdh.ibid.product.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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
        Page<Product> products = productRepository.findAllByOrderByIdDesc(PageRequest.of(0, 10));

        // then
        assertThat(products.getContent()).extracting(Product::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @DisplayName("페이지 크기만큼만 조회하고 다음 페이지 존재 여부를 알려준다")
    @Test
    void findAllByOrderByIdDesc_paging() {
        // given
        for (int i = 0; i < 3; i++) {
            productRepository.save(Product.create(SELLER_ID, "상품" + i, "상태 좋음", 10000, 1));
        }

        // when
        Page<Product> firstPage = productRepository.findAllByOrderByIdDesc(PageRequest.of(0, 2));

        // then
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.hasNext()).isTrue();
    }
}
