package project.kjhjdh.ibid.product.presentation;

import project.kjhjdh.ibid.product.DeviceSpecFixture;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import org.springframework.test.util.ReflectionTestUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.application.ProductWithThumbnail;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.support.ControllerTestSupport;

class ProductLikeControllerTest extends ControllerTestSupport {

    @DisplayName("찜하면 200을 응답한다")
    @Test
    void like() {
        // given
        willDoNothing().given(productLikeService).like(anyLong(), eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/products/{productId}/like", 1L)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @DisplayName("존재하지 않는 상품을 찜하면 404를 응답한다")
    @Test
    void like_notFound() {
        // given
        willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                .given(productLikeService).like(anyLong(), eq(999L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/products/{productId}/like", 999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("PRODUCT_NOT_FOUND"));
    }

    @DisplayName("동시 요청으로 유니크 제약을 위반하면 500이 아닌 409를 응답한다")
    @Test
    void like_duplicate() {
        // given
        willThrow(new DataIntegrityViolationException("uk_product_like_user_product"))
                .given(productLikeService).like(anyLong(), eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/products/{productId}/like", 1L)
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo("DATA_INTEGRITY_VIOLATION"));
    }

    @DisplayName("찜 상태 조회에 성공하면 200과 찜수·내 찜여부를 응답한다")
    @Test
    void likeStatus() {
        // given
        given(productLikeService.countLikes(1L)).willReturn(7L);
        given(productLikeService.isLiked(anyLong(), eq(1L))).willReturn(true);

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/products/{productId}/like", 1L)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("count", equalTo(7))
                .body("liked", equalTo(true));
    }

    @DisplayName("찜을 취소하면 204를 응답한다")
    @Test
    void unlike() {
        // given
        willDoNothing().given(productLikeService).unlike(anyLong(), eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .delete("/api/products/{productId}/like", 1L)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("관심목록 조회에 성공하면 200과 상품 요약 목록을 응답한다")
    @Test
    void myLikes() {
        // given
        Product product = Product.create(5L, "나이키 후드", "상태 좋음", 89000, ProductCondition.LIKE_NEW, DeviceSpecFixture.sample());
        ReflectionTestUtils.setField(product, "id", 10L);
        given(productLikeService.myLikedProducts(anyLong())).willReturn(List.of(
                new ProductWithThumbnail(product, "https://image/a.jpg")
        ));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/products/me/likes")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("[0].productId", equalTo(10))
                .body("[0].thumbnailUrl", equalTo("https://image/a.jpg"));
    }
}
