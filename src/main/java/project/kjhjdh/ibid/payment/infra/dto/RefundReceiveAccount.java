package project.kjhjdh.ibid.payment.infra.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundReceiveAccount {

	private String bankCode;
	private String accountNumber;
	private String holderName;
}
