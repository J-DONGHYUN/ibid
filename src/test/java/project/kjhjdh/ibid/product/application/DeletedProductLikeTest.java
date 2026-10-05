package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

class DeletedProductLikeTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long USER_ID = 20L;

    @Autowired
    private ProductLikeService productLikeService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @DisplayName("[PD-07] 삭제된 상품도 찜할 수 있다 (QA-1.5 — 현재 동작. 요구사항에 답이 없다)")
    @Test
    void like_deletedProduct() {
        // given — 판매자가 삭제한 상품
        Long productId = saveProduct();
        productService.delete(SELLER_ID, productId);

        // when — 찜한다
        productLikeService.like(USER_ID, productId);

        // then — 찜은 되고 찜 수에도 센다. ProductLikeService.like 는 existsById 만 보고
        //        소프트 삭제라 행이 남아 있어서다
        assertThat(productLikeService.isLiked(USER_ID, productId)).isTrue();
        assertThat(productLikeService.countLikes(productId)).isEqualTo(1);
    }

    @DisplayName("[PD-08] 삭제된 상품은 관심 목록에 나오지 않는다 (QA-1.5)")
    @Test
    void myLikedProducts_excludesDeleted() {
        // given — 찜한 뒤 판매자가 삭제한다
        Long productId = saveProduct();
        productLikeService.like(USER_ID, productId);
        productService.delete(SELLER_ID, productId);

        // when
        var liked = productLikeService.myLikedProducts(USER_ID);

        // then — 관심 목록에서는 걸러진다. 찜 행은 남아 사용자 눈에만 안 띈다
        assertThat(liked).isEmpty();
        assertThat(productLikeService.isLiked(USER_ID, productId)).isTrue();
    }

    private Long saveProduct() {
        return productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
    }
}
