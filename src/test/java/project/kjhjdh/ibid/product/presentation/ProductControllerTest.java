package project.kjhjdh.ibid.product.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.presentation.dto.ImageConfirmRequest;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignRequest;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductDetailResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductListResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductRegisterRequest;
import project.kjhjdh.ibid.product.presentation.dto.ProductSummaryResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductUpdateRequest;
import project.kjhjdh.ibid.support.ControllerTestSupport;

class ProductControllerTest extends ControllerTestSupport {

    @DisplayName("판매 등록에 성공하면 201과 productId를 응답한다")
    @Test
    void register() {
        // given
        given(productService.register(anyLong(), any())).willReturn(10L);

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ProductRegisterRequest("나이키 후드", "상태 좋음", 89000, 3, ProductCondition.LIKE_NEW))
                .when()
                .post("/api/products")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("productId", equalTo(10));
    }

    @DisplayName("제목이 비어 있으면 400을 응답한다")
    @Test
    void register_blankTitle() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ProductRegisterRequest("", "상태 좋음", 89000, 3, ProductCondition.LIKE_NEW))
                .when()
                .post("/api/products")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("판매가가 0 이하이면 400을 응답한다")
    @Test
    void register_invalidPrice() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ProductRegisterRequest("나이키 후드", "상태 좋음", 0, 3, ProductCondition.LIKE_NEW))
                .when()
                .post("/api/products")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("재고 수량이 0 이하이면 400을 응답한다")
    @Test
    void register_invalidStock() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ProductRegisterRequest("나이키 후드", "상태 좋음", 89000, 0, ProductCondition.LIKE_NEW))
                .when()
                .post("/api/products")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("상품 목록 조회에 성공하면 200과 상품 목록을 응답한다")
    @Test
    void getProducts() {
        // given
        given(productService.getProducts(any())).willReturn(new ProductListResponse(
                List.of(
                        new ProductSummaryResponse(2L, "나이키 후드", 89000, 3, ProductStatus.ON_SALE, "https://image/thumb.jpg"),
                        new ProductSummaryResponse(1L, "아디다스 슬리퍼", 30000, 0, ProductStatus.SOLD_OUT, null)
                ),
                1L, true
        ));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("products.size()", equalTo(2))
                .body("products[0].productId", equalTo(2))
                .body("products[0].stock", equalTo(3))
                .body("products[0].status", equalTo("ON_SALE"))
                .body("products[0].thumbnailUrl", equalTo("https://image/thumb.jpg"))
                .body("products[1].status", equalTo("SOLD_OUT"))
                .body("nextCursor", equalTo(1))
                .body("hasNext", equalTo(true));
    }

    @DisplayName("상품 상세 조회에 성공하면 200과 상품 정보를 응답한다")
    @Test
    void getProduct() {
        // given
        given(productService.getProduct(1L)).willReturn(
                new ProductDetailResponse(1L, 5L, "나이키 후드", "상태 좋음", 89000, 3, ProductStatus.ON_SALE,
                        ProductCondition.LIKE_NEW, List.of("https://image/a.jpg")));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/products/{productId}", 1L)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("productId", equalTo(1))
                .body("sellerId", equalTo(5))
                .body("title", equalTo("나이키 후드"))
                .body("description", equalTo("상태 좋음"))
                .body("stock", equalTo(3))
                .body("status", equalTo("ON_SALE"))
                .body("condition", equalTo("LIKE_NEW"))
                .body("imageUrls[0]", equalTo("https://image/a.jpg"));
    }

    @DisplayName("presigned URL 발급에 성공하면 200과 URL 목록을 응답한다")
    @Test
    void presignImages() {
        // given
        given(productService.generatePresignedUrls(anyLong(), eq(1L), any()))
                .willReturn(List.of(new ImagePresignResponse("https://presigned", "products/1/uuid.jpg", "https://image")));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(List.of(new ImagePresignRequest("a.jpg", "image/jpeg")))
                .when()
                .post("/api/products/{productId}/images/presign", 1L)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("[0].presignedUrl", equalTo("https://presigned"))
                .body("[0].imageUrl", equalTo("https://image"));
    }

    @DisplayName("이미지 확정 저장에 성공하면 200을 응답한다")
    @Test
    void confirmImages() {
        // given
        willDoNothing().given(productService).confirmImages(anyLong(), eq(1L), any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ImageConfirmRequest(List.of("https://image1", "https://image2")))
                .when()
                .post("/api/products/{productId}/images/confirm", 1L)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @DisplayName("본인 상품이 아니면 이미지 확정 시 403을 응답한다")
    @Test
    void confirmImages_forbidden() {
        // given
        willThrow(new BusinessException(ErrorCode.ACCESS_DENIED))
                .given(productService).confirmImages(anyLong(), eq(1L), any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ImageConfirmRequest(List.of("https://image1")))
                .when()
                .post("/api/products/{productId}/images/confirm", 1L)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", equalTo("ACCESS_DENIED"));
    }

    @DisplayName("상품 수정에 성공하면 200을 응답한다")
    @Test
    void update() {
        // given
        willDoNothing().given(productService).update(anyLong(), eq(1L), any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ProductUpdateRequest("수정", "수정 설명", 50000, 2, ProductCondition.USED))
                .when()
                .patch("/api/products/{productId}", 1L)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @DisplayName("상품 삭제에 성공하면 204를 응답한다")
    @Test
    void delete() {
        // given
        willDoNothing().given(productService).delete(anyLong(), eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .delete("/api/products/{productId}", 1L)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("거래된 상품을 삭제하면 409를 응답한다")
    @Test
    void delete_conflict() {
        // given
        willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_MODIFIABLE))
                .given(productService).delete(anyLong(), eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .delete("/api/products/{productId}", 1L)
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo("PRODUCT_NOT_MODIFIABLE"));
    }

    @DisplayName("판매 시작에 성공하면 200을 응답한다")
    @Test
    void openForSale() {
        // given
        willDoNothing().given(productService).openForSale(anyLong(), eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .patch("/api/products/{productId}/on-sale", 1L)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @DisplayName("등록한 판매자가 아니면 판매 시작 시 403을 응답한다")
    @Test
    void openForSale_forbidden() {
        // given
        willThrow(new BusinessException(ErrorCode.ACCESS_DENIED))
                .given(productService).openForSale(anyLong(), eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .patch("/api/products/{productId}/on-sale", 1L)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", equalTo("ACCESS_DENIED"));
    }

    @DisplayName("판매 대기 상태가 아닌 상품의 판매를 시작하면 409를 응답한다")
    @Test
    void openForSale_notPending() {
        // given
        willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_PENDING))
                .given(productService).openForSale(anyLong(), eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .patch("/api/products/{productId}/on-sale", 1L)
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo("PRODUCT_NOT_PENDING"));
    }

    @DisplayName("존재하지 않는 상품을 조회하면 404를 응답한다")
    @Test
    void getProduct_notFound() {
        // given
        willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                .given(productService).getProduct(eq(999L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/products/{productId}", 999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("PRODUCT_NOT_FOUND"));
    }
}
