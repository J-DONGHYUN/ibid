package project.kjhjdh.ibid.product.infra;

import project.kjhjdh.ibid.product.domain.ProductCondition;

import project.kjhjdh.ibid.product.DeviceSpecFixture;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.support.RepositoryTestSupport;

class ProductRepositoryTest extends RepositoryTestSupport {

    private static final Long SELLER_ID = 1L;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("커서(id)보다 작은 상품만 최신 등록순으로 조회한다")
    @Test
    void findByIdLessThanOrderByIdDesc() {
        // given
        Product first = productRepository.save(Product.create(SELLER_ID, "첫번째", "상태 좋음", 10000, ProductCondition.USED, DeviceSpecFixture.sample()));
        Product second = productRepository.save(Product.create(SELLER_ID, "두번째", "상태 좋음", 20000, ProductCondition.USED, DeviceSpecFixture.sample()));
        Product third = productRepository.save(Product.create(SELLER_ID, "세번째", "상태 좋음", 30000, ProductCondition.USED, DeviceSpecFixture.sample()));

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
            productRepository.save(Product.create(SELLER_ID, "상품" + i, "상태 좋음", 10000, ProductCondition.USED, DeviceSpecFixture.sample()));
        }

        // when
        Slice<Product> slice = productRepository.findByIdLessThanOrderByIdDesc(Long.MAX_VALUE, PageRequest.of(0, 2));

        // then
        assertThat(slice.getContent()).hasSize(2);
        assertThat(slice.hasNext()).isTrue();
    }

    @DisplayName("[PD-03] 거래완료를 뺀 상품만 커서로 조회한다")
    @Test
    void findByStatusNotAndIdLessThanOrderByIdDesc() {
        // given — 판매중·예약중·거래완료 각 하나
        Product onSale = productRepository.save(Product.create(SELLER_ID, "판매중", "상태 좋음", 10000, ProductCondition.USED, DeviceSpecFixture.sample()));
        Product reserved = Product.create(SELLER_ID, "예약중", "상태 좋음", 20000, ProductCondition.USED, DeviceSpecFixture.sample());
        reserved.reserve(2L);
        reserved = productRepository.save(reserved);
        Product sold = Product.create(SELLER_ID, "거래완료", "상태 좋음", 30000, ProductCondition.USED, DeviceSpecFixture.sample());
        sold.complete(3L);
        productRepository.save(sold);

        // when
        Slice<Product> slice = productRepository.findByStatusNotAndIdLessThanOrderByIdDesc(
                ProductStatus.SOLD, Long.MAX_VALUE, PageRequest.of(0, 10));

        // then — 거래완료는 빠지고 판매중·예약중만, 최신순
        assertThat(slice.getContent()).extracting(Product::getId)
                .containsExactly(reserved.getId(), onSale.getId());
    }

    @DisplayName("[PD-03] 거래완료 제외 조회도 커서 경계에서 겹치거나 빠지지 않는다")
    @Test
    void findByStatusNotAndIdLessThanOrderByIdDesc_cursor() {
        // given — 판매중·거래완료·판매중·판매중 순 (거래완료가 윈도 중간에 낀다)
        Product p1 = productRepository.save(Product.create(SELLER_ID, "판매중1", "상태 좋음", 10000, ProductCondition.USED, DeviceSpecFixture.sample()));
        Product sold = Product.create(SELLER_ID, "거래완료", "상태 좋음", 20000, ProductCondition.USED, DeviceSpecFixture.sample());
        sold.complete(9L);
        productRepository.save(sold);
        Product p3 = productRepository.save(Product.create(SELLER_ID, "판매중3", "상태 좋음", 30000, ProductCondition.USED, DeviceSpecFixture.sample()));
        Product p4 = productRepository.save(Product.create(SELLER_ID, "판매중4", "상태 좋음", 40000, ProductCondition.USED, DeviceSpecFixture.sample()));

        // when — 첫 페이지(크기 2)
        Slice<Product> first = productRepository.findByStatusNotAndIdLessThanOrderByIdDesc(
                ProductStatus.SOLD, Long.MAX_VALUE, PageRequest.of(0, 2));

        // then — 최신 판매중 둘, 다음 있음
        assertThat(first.getContent()).extracting(Product::getId).containsExactly(p4.getId(), p3.getId());
        assertThat(first.hasNext()).isTrue();

        // when — 마지막(p3) 커서로 이어 받으면 거래완료는 건너뛰고 p1 만
        Slice<Product> next = productRepository.findByStatusNotAndIdLessThanOrderByIdDesc(
                ProductStatus.SOLD, p3.getId(), PageRequest.of(0, 2));

        // then — 겹침·빠짐 없이 p1 만, 다음 없음
        assertThat(next.getContent()).extracting(Product::getId).containsExactly(p1.getId());
        assertThat(next.hasNext()).isFalse();
    }

    @DisplayName("조회수를 delta만큼 증가시킨다")
    @Test
    void increaseViewCount() {
        // given
        Product product = productRepository.save(Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, ProductCondition.USED, DeviceSpecFixture.sample()));

        // when
        int updated = productRepository.increaseViewCount(product.getId(), 5L);

        // then
        assertThat(updated).isEqualTo(1);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getViewCount()).isEqualTo(5L);
    }

    @DisplayName("존재하지 않는 상품의 조회수 증가는 아무 행도 바꾸지 않는다")
    @Test
    void increaseViewCount_notFound() {
        // when
        int updated = productRepository.increaseViewCount(999L, 5L);

        // then
        assertThat(updated).isZero();
    }
}
