package project.kjhjdh.ibid.payment.infra.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CashReceipt {

	private String type;
	private String receiptKey;
	private String issueNumber;
	private String receiptUrl;
	private Integer amount;
	private Integer taxFreeAmount;

}
