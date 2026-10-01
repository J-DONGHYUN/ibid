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
import project.kjhjdh.ibid.trade.presentation.dto.CompleteRequest;

class CompletionControllerTest extends ControllerTestSupport {

    private static final Long PRODUCT_ID = 1L;
    private static final Long BUYER_ID = 20L;

    @DisplayName("[TR-03] 거래완료에 성공하면 200을 응답하고 서비스에 판매자와 상대를 넘긴다")
    @Test
    void complete() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new CompleteRequest(BUYER_ID))
                .when()
                .post("/api/products/{productId}/completion", PRODUCT_ID)
                .then()
                .statusCode(200);

        then(completionService).should().complete(eq(PRODUCT_ID), anyLong(), eq(BUYER_ID));
    }

    @DisplayName("[TR-03] 거래 상대를 보내지 않으면 400을 응답한다")
    @Test
    void complete_noBuyer() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new CompleteRequest(null))
                .when()
                .post("/api/products/{productId}/completion", PRODUCT_ID)
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("[TR-03] 판매자 본인이 아니면 403을 응답한다")
    @Test
    void complete_notOwner() {
        // given
        willThrow(new BusinessException(ErrorCode.ACCESS_DENIED))
                .given(completionService).complete(anyLong(), anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new CompleteRequest(BUYER_ID))
                .when()
                .post("/api/products/{productId}/completion", PRODUCT_ID)
                .then()
                .statusCode(403)
                .body("code", equalTo("ACCESS_DENIED"));
    }

    @DisplayName("[I-04] 채팅 상대가 아니면 400을 응답한다")
    @Test
    void complete_notChatPartner() {
        // given
        willThrow(new BusinessException(ErrorCode.NOT_CHAT_PARTNER))
                .given(completionService).complete(anyLong(), anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new CompleteRequest(BUYER_ID))
                .when()
                .post("/api/products/{productId}/completion", PRODUCT_ID)
                .then()
                .statusCode(400)
                .body("code", equalTo("NOT_CHAT_PARTNER"));
    }

    @DisplayName("[I-03] 이미 거래완료된 상품이면 409를 응답한다")
    @Test
    void complete_alreadySold() {
        // given
        willThrow(new BusinessException(ErrorCode.PRODUCT_ALREADY_SOLD))
                .given(completionService).complete(anyLong(), anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new CompleteRequest(BUYER_ID))
                .when()
                .post("/api/products/{productId}/completion", PRODUCT_ID)
                .then()
                .statusCode(409)
                .body("code", equalTo("PRODUCT_ALREADY_SOLD"));
    }

    @DisplayName("[I-12] 동시 전이로 버전이 충돌하면 409를 응답한다")
    @Test
    void complete_concurrentConflict() {
        // given
        willThrow(new OptimisticLockingFailureException("version conflict"))
                .given(completionService).complete(anyLong(), anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new CompleteRequest(BUYER_ID))
                .when()
                .post("/api/products/{productId}/completion", PRODUCT_ID)
                .then()
                .statusCode(409)
                .body("code", equalTo("CONCURRENT_UPDATE"));
    }
}
