package project.kjhjdh.ibid.payment.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.payment.application.PaymentService;
import project.kjhjdh.ibid.payment.infra.dto.PaymentTossDtoImpl;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateResponse;
import project.kjhjdh.ibid.support.ControllerTestSupport;

class PaymentControllerTest extends ControllerTestSupport {

	@MockitoBean
	private PaymentService paymentService;

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

	@DisplayName("결제 승인에 실패하면 400과 PAYMENT_CONFIRM_FAILED를 응답한다")
	@Test
	void confirm_failed() {
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
				.statusCode(HttpStatus.BAD_REQUEST.value())
				.body("code", equalTo("PAYMENT_CONFIRM_FAILED"));
	}

	private PaymentTossDtoImpl tossDto(String status) {
		PaymentTossDtoImpl dto = new PaymentTossDtoImpl();
		dto.setStatus(status);
		return dto;
	}
}
