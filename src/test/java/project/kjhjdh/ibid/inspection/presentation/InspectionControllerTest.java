package project.kjhjdh.ibid.inspection.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.support.ControllerTestSupport;

class InspectionControllerTest extends ControllerTestSupport {

    @DisplayName("검수 수령 처리하면 200을 응답한다")
    @Test
    void receive() {
        // given
        willDoNothing().given(inspectionService).receive(eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/inspections/{orderId}/receive", 1L)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @DisplayName("존재하지 않는 주문을 수령하면 404를 응답한다")
    @Test
    void receive_orderNotFound() {
        // given
        willThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND))
                .given(inspectionService).receive(eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/inspections/{orderId}/receive", 1L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("ORDER_NOT_FOUND"));
    }

    @DisplayName("검수 시작할 수 없는 상태면 409를 응답한다")
    @Test
    void receive_notInspectable() {
        // given
        willThrow(new BusinessException(ErrorCode.ORDER_NOT_INSPECTABLE))
                .given(inspectionService).receive(eq(1L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/inspections/{orderId}/receive", 1L)
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo("ORDER_NOT_INSPECTABLE"));
    }
}
