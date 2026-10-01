package project.kjhjdh.ibid.trade.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.support.ControllerTestSupport;
import project.kjhjdh.ibid.trade.presentation.dto.ReserveRequest;

class ReservationControllerTest extends ControllerTestSupport {

    private static final Long PRODUCT_ID = 1L;
    private static final Long BUYER_ID = 20L;

    @DisplayName("[TR-01] 예약에 성공하면 200을 응답하고 서비스에 판매자와 상대를 넘긴다")
    @Test
    void reserve() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ReserveRequest(BUYER_ID))
                .when()
                .post("/api/products/{productId}/reservation", PRODUCT_ID)
                .then()
                .statusCode(200);

        then(reservationService).should().reserve(eq(PRODUCT_ID), anyLong(), eq(BUYER_ID));
    }

    @DisplayName("[TR-01] 예약 상대를 보내지 않으면 400을 응답한다")
    @Test
    void reserve_noBuyer() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ReserveRequest(null))
                .when()
                .post("/api/products/{productId}/reservation", PRODUCT_ID)
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("[TR-01] 판매자 본인이 아니면 403을 응답한다")
    @Test
    void reserve_notOwner() {
        // given
        willThrow(new BusinessException(ErrorCode.ACCESS_DENIED))
                .given(reservationService).reserve(anyLong(), anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ReserveRequest(BUYER_ID))
                .when()
                .post("/api/products/{productId}/reservation", PRODUCT_ID)
                .then()
                .statusCode(403)
                .body("code", equalTo("ACCESS_DENIED"));
    }

    @DisplayName("[I-04] 채팅 상대가 아니면 400을 응답한다")
    @Test
    void reserve_notChatPartner() {
        // given
        willThrow(new BusinessException(ErrorCode.NOT_CHAT_PARTNER))
                .given(reservationService).reserve(anyLong(), anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ReserveRequest(BUYER_ID))
                .when()
                .post("/api/products/{productId}/reservation", PRODUCT_ID)
                .then()
                .statusCode(400)
                .body("code", equalTo("NOT_CHAT_PARTNER"));
    }

    @DisplayName("[I-12] 동시 예약으로 버전이 충돌하면 409를 응답한다")
    @Test
    void reserve_concurrentConflict() {
        // given
        willThrow(new OptimisticLockingFailureException("version conflict"))
                .given(reservationService).reserve(anyLong(), anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ReserveRequest(BUYER_ID))
                .when()
                .post("/api/products/{productId}/reservation", PRODUCT_ID)
                .then()
                .statusCode(409)
                .body("code", equalTo("CONCURRENT_UPDATE"));
    }

    @DisplayName("[TR-02] 예약 해제에 성공하면 200을 응답한다")
    @Test
    void cancelReservation() {
        // when & then
        RestAssuredMockMvc.given()
                .when()
                .delete("/api/products/{productId}/reservation", PRODUCT_ID)
                .then()
                .statusCode(200);

        then(reservationService).should().cancelReservation(eq(PRODUCT_ID), anyLong());
    }

    @DisplayName("[TR-02] 예약중이 아닌 상품을 해제하면 409를 응답한다")
    @Test
    void cancelReservation_notReserved() {
        // given
        willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_RESERVED))
                .given(reservationService).cancelReservation(anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .delete("/api/products/{productId}/reservation", PRODUCT_ID)
                .then()
                .statusCode(409)
                .body("code", equalTo("PRODUCT_NOT_RESERVED"));
    }
}
