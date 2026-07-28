package project.kjhjdh.ibid.payment.infra;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.web.client.RestClient;

class TossPaymentClientTest {

	TossPaymentClient tossPaymentClient;

	@BeforeEach
	void setUp() {
		tossPaymentClient = new TossPaymentClient(RestClient.builder().build());
	}
}