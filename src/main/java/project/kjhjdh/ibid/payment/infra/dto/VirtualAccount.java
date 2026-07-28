package project.kjhjdh.ibid.payment.infra.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VirtualAccount {
	private String accountType;
	private String accountNumber;
	private String bankCode;
	private String customerName;
	private String dueDate;
	private String refundStatus;
	private boolean expired;
	private String settlementStatus;
	private RefundReceiveAccount refundReceiveAccount;

}
