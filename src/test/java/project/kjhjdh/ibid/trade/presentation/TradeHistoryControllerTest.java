package project.kjhjdh.ibid.trade.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.application.ProductListResult;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.support.ControllerTestSupport;

class TradeHistoryControllerTest extends ControllerTestSupport {

    @DisplayName("[TR-04] 판매 내역을 조회하면 200과 내 상품 목록을 응답한다")
    @Test
    void sales() {
        // given
        given(tradeHistoryService.getSales(anyLong(), isNull(), any()))
                .willReturn(result(product(5L)));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/products/me/sales")
                .then()
                .statusCode(200)
                .body("products[0].productId", equalTo(5))
                .body("products[0].status", equalTo("ON_SALE"));
    }

    @DisplayName("[TR-04] 상태를 주면 그 상태로 걸러 조회한다")
    @Test
    void sales_statusFilter() {
        // given
        given(tradeHistoryService.getSales(anyLong(), eq(ProductStatus.SOLD), any()))
                .willReturn(result(product(5L)));

        // when
        RestAssuredMockMvc.given()
                .queryParam("status", "SOLD")
                .when()
                .get("/api/products/me/sales")
                .then()
                .statusCode(200);

        // then
        then(tradeHistoryService).should().getSales(anyLong(), eq(ProductStatus.SOLD), any());
    }

    @DisplayName("[TR-05] 구매 내역을 조회하면 200과 내가 거래 상대인 상품 목록을 응답한다")
    @Test
    void purchases() {
        // given
        given(tradeHistoryService.getPurchases(anyLong(), any()))
                .willReturn(result(product(7L)));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/products/me/purchases")
                .then()
                .statusCode(200)
                .body("products[0].productId", equalTo(7));
    }

    private ProductListResult result(Product product) {
        return new ProductListResult(
                new SliceImpl<>(List.of(product), PageRequest.of(0, 16), false),
                Map.of(product.getId(), "https://thumb"));
    }

    private Product product(Long id) {
        Product product = Product.create(10L, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample());
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
