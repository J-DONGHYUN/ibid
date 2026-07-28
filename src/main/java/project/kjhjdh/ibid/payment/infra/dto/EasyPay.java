package project.kjhjdh.ibid.payment.infra.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EasyPay {

	private String provider;
	private Integer amount;
	private Integer discountAmount;
}
