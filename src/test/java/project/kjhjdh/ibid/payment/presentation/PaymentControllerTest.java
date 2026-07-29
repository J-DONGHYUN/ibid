package project.kjhjdh.ibid.payment.presentation;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.payment.infra.dto.PaymentTossDtoImpl;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateResponse;
import project.kjhjdh.ibid.support.ControllerTestSupport;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class PaymentControllerTest extends ControllerTestSupport {

	@DisplayName("결제를 생성하면 201과 paymentId를 응답한다")
	@Test
	void create() {
		// given
		given(paymentService.create(any())).willReturn(new PaymentCreateResponse(1L));

		// when & then
		RestAssuredMockMvc.given()
				.contentType(ContentType.JSON)
				.body(new PaymentCreateRequest(10L))
				.when()
				.post("/api/payments")
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("paymentId", equalTo(1));
	}

	@DisplayName("생성 요청의 필수 값이 비어 있으면 400과 INVALID_INPUT을 응답한다")
	@Test
	void create_invalidRequest() {
		// when & then
		RestAssuredMockMvc.given()
				.contentType(ContentType.JSON)
				.body(new PaymentCreateRequest(null))
				.when()
				.post("/api/payments")
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value())
				.body("code", equalTo("INVALID_INPUT"));
	}

	@DisplayName("결제 승인에 성공하면 200과 승인 결과를 응답한다")
	@Test
	void confirm() {
		// given
		given(paymentService.confirm(any(), any())).willReturn(tossDto("DONE"));

		// when & then
		RestAssuredMockMvc.given()
				.contentType(ContentType.JSON)
				.body(new PaymentConfirmRequest("toss-order-1", "50000", "payment-key-1"))
				.when()
				.post("/api/payments/1/confirm")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("status", equalTo("DONE"));
	}

	@DisplayName("승인 요청의 필수 값이 비어 있으면 400과 INVALID_INPUT을 응답한다")
	@Test
	void confirm_invalidRequest() {
		// when & then
		RestAssuredMockMvc.given()
				.contentType(ContentType.JSON)
				.body(new PaymentConfirmRequest("", "50000", "payment-key-1"))
				.when()
				.post("/api/payments/1/confirm")
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value())
				.body("code", equalTo("INVALID_INPUT"));
	}

	@DisplayName("결제 승인에 실패하면 400과 INVALID_PAYMENT_CONFIRM을 응답한다")
	@Test
	void confirm_failed() {
		// given
		given(paymentService.confirm(any(), any()))
				.willThrow(new BusinessException(ErrorCode.INVALID_PAYMENT_CONFIRM));

		// when & then
		RestAssuredMockMvc.given()
				.contentType(ContentType.JSON)
				.body(new PaymentConfirmRequest("toss-order-1", "50000", "payment-key-1"))
				.when()
				.post("/api/payments/1/confirm")
				.then()
				.statusCode(HttpStatus.BAD_REQUEST.value())
				.body("code", equalTo("INVALID_PAYMENT_CONFIRM"));
	}

	@DisplayName("토스 승인 자체가 실패하면 500과 PAYMENT_CONFIRM_FAILED를 응답한다")
	@Test
	void confirm_tossConfirmFailed() {
		// given
		given(paymentService.confirm(any(), any()))
				.willThrow(new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED));

		// when & then
		RestAssuredMockMvc.given()
				.contentType(ContentType.JSON)
				.body(new PaymentConfirmRequest("toss-order-1", "50000", "payment-key-1"))
				.when()
				.post("/api/payments/1/confirm")
				.then()
				.statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.body("code", equalTo("PAYMENT_CONFIRM_FAILED"));
	}

	@DisplayName("결제 실패를 알리면 200을 응답하고 해당 결제의 주문을 취소한다")
	@Test
	void fail() {
		// when & then
		RestAssuredMockMvc.given()
				.when()
				.post("/api/payments/1/fail")
				.then()
				.statusCode(HttpStatus.OK.value());

		verify(paymentService).fail(1L);
	}

	@DisplayName("존재하지 않는 결제를 실패 처리하면 404와 PAYMENT_NOT_FOUND를 응답한다")
	@Test
	void fail_paymentNotFound() {
		// given
		doThrow(new BusinessException(ErrorCode.PAYMENT_NOT_FOUND)).when(paymentService).fail(999L);

		// when & then
		RestAssuredMockMvc.given()
				.when()
				.post("/api/payments/999/fail")
				.then()
				.statusCode(HttpStatus.NOT_FOUND.value())
				.body("code", equalTo("PAYMENT_NOT_FOUND"));
	}

	private PaymentTossDtoImpl tossDto(String status) {
		PaymentTossDtoImpl dto = new PaymentTossDtoImpl();
		dto.setStatus(status);
		return dto;
	}
}
