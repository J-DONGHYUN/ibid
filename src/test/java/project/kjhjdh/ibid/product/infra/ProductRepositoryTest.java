package project.kjhjdh.ibid.product.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.support.RepositoryTestSupport;

class ProductRepositoryTest extends RepositoryTestSupport {

    private static final Long SELLER_ID = 1L;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("커서(id)보다 작은 상품만 최신 등록순으로 조회한다")
    @Test
    void findByIdLessThanOrderByIdDesc() {
        // given
        Product first = productRepository.save(Product.create(SELLER_ID, "첫번째", "상태 좋음", 10000, 1));
        Product second = productRepository.save(Product.create(SELLER_ID, "두번째", "상태 좋음", 20000, 2));
        Product third = productRepository.save(Product.create(SELLER_ID, "세번째", "상태 좋음", 30000, 3));

        // when
        Slice<Product> slice = productRepository.findByIdLessThanOrderByIdDesc(third.getId(), PageRequest.of(0, 10));

        // then
        assertThat(slice.getContent()).extracting(Product::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @DisplayName("조회 크기보다 상품이 많으면 다음 페이지가 있다고 알려준다")
    @Test
    void findByIdLessThanOrderByIdDesc_hasNext() {
        // given
        for (int i = 0; i < 3; i++) {
            productRepository.save(Product.create(SELLER_ID, "상품" + i, "상태 좋음", 10000, 1));
        }

        // when
        Slice<Product> slice = productRepository.findByIdLessThanOrderByIdDesc(Long.MAX_VALUE, PageRequest.of(0, 2));

        // then
        assertThat(slice.getContent()).hasSize(2);
        assertThat(slice.hasNext()).isTrue();
    }
}
