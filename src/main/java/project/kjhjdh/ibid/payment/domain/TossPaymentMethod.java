package project.kjhjdh.ibid.payment.domain;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum TossPaymentMethod {

	CARD("카드"),
	VIRTUAL_ACCOUNT("가상계좌"),
	EASY_PAY("간편결제"),
	MOBILE_PHONE("휴대폰"),
	TRANSFER("계좌이체"),
	CULTURE_GIFT_CERTIFICATE("문화상품권"),
	BOOK_GIFT_CERTIFICATE("도서문화상품권"),
	GAME_GIFT_CERTIFICATE("게임문화상품권");

	private final String label;

	public static TossPaymentMethod from(String label) {
		return Arrays.stream(values())
			.filter(method -> method.label.equals(label))
			.findFirst()
			.orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED));
	}
}
