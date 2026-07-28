package project.kjhjdh.ibid.order.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.application.PurchaseResult;
import project.kjhjdh.ibid.order.domain.OrderStatus;
import project.kjhjdh.ibid.order.presentation.dto.MyOrdersResponse;
import project.kjhjdh.ibid.order.presentation.dto.OrderDetailResponse;
import project.kjhjdh.ibid.order.presentation.dto.OrderSummaryResponse;
import project.kjhjdh.ibid.order.presentation.dto.PurchaseRequest;
import project.kjhjdh.ibid.support.ControllerTestSupport;

class OrderControllerTest extends ControllerTestSupport {

    @DisplayName("즉시구매에 성공하면 201과 orderId를 응답한다")
    @Test
    void purchase() {
        // given
        given(orderService.purchase(anyLong(), any())).willReturn(new PurchaseResult(100L, 178000));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new PurchaseRequest(1L, 2))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("orderId", equalTo(100));
    }

    @DisplayName("구매 수량이 0 이하이면 400을 응답한다")
    @Test
    void purchase_invalidQuantity() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new PurchaseRequest(1L, 0))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("본인이 등록한 상품을 구매하면 400을 응답한다")
    @Test
    void purchase_selfTrade() {
        // given
        willThrow(new BusinessException(ErrorCode.SELF_TRADE_NOT_ALLOWED))
                .given(orderService).purchase(anyLong(), any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new PurchaseRequest(1L, 1))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("SELF_TRADE_NOT_ALLOWED"));
    }

    @DisplayName("재고가 부족하면 409를 응답한다")
    @Test
    void purchase_insufficientStock() {
        // given
        willThrow(new BusinessException(ErrorCode.INSUFFICIENT_STOCK))
                .given(orderService).purchase(anyLong(), any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new PurchaseRequest(1L, 99))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo("INSUFFICIENT_STOCK"));
    }

    @DisplayName("존재하지 않는 상품을 구매하면 404를 응답한다")
    @Test
    void purchase_productNotFound() {
        // given
        willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                .given(orderService).purchase(anyLong(), any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new PurchaseRequest(999L, 1))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("PRODUCT_NOT_FOUND"));
    }

    @DisplayName("판매자가 발송 처리하면 200을 응답한다")
    @Test
    void ship() {
        // given
        willDoNothing().given(orderService).ship(anyLong(), eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/orders/{orderId}/ship", 1L)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @DisplayName("판매자가 아니면 발송 시 403을 응답한다")
    @Test
    void ship_forbidden() {
        // given
        willThrow(new BusinessException(ErrorCode.ACCESS_DENIED))
                .given(orderService).ship(anyLong(), eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/orders/{orderId}/ship", 1L)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", equalTo("ACCESS_DENIED"));
    }

    @DisplayName("발송할 수 없는 상태의 주문을 발송하면 409를 응답한다")
    @Test
    void ship_notShippable() {
        // given
        willThrow(new BusinessException(ErrorCode.ORDER_NOT_SHIPPABLE))
                .given(orderService).ship(anyLong(), eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/orders/{orderId}/ship", 1L)
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo("ORDER_NOT_SHIPPABLE"));
    }

    @DisplayName("내 거래 목록 조회에 성공하면 200과 목록을 응답한다")
    @Test
    void getMyOrders() {
        // given
        given(orderService.getMyOrders(anyLong(), any())).willReturn(new MyOrdersResponse(List.of(
                new OrderSummaryResponse(100L, 2L, "나이키 후드", 1, 89000, OrderStatus.PAID)
        )));

        // when & then
        RestAssuredMockMvc.given()
                .queryParam("role", "buyer")
                .when()
                .get("/api/orders")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("orders.size()", equalTo(1))
                .body("orders[0].productTitle", equalTo("나이키 후드"))
                .body("orders[0].status", equalTo("PAID"));
    }

    @DisplayName("내 거래 상세 조회에 성공하면 200과 상세를 응답한다")
    @Test
    void getMyOrder() {
        // given
        given(orderService.getMyOrder(anyLong(), eq(100L))).willReturn(
                new OrderDetailResponse(100L, 2L, "나이키 후드", 1L, 3L, 1, 89000, OrderStatus.UNDER_INSPECTION));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/orders/{orderId}", 100L)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("orderId", equalTo(100))
                .body("status", equalTo("UNDER_INSPECTION"));
    }

    @DisplayName("거래 당사자가 아니면 상세 조회 시 403을 응답한다")
    @Test
    void getMyOrder_forbidden() {
        // given
        willThrow(new BusinessException(ErrorCode.ACCESS_DENIED))
                .given(orderService).getMyOrder(anyLong(), eq(100L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/orders/{orderId}", 100L)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", equalTo("ACCESS_DENIED"));
    }
}
