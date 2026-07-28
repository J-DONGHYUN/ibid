package project.kjhjdh.ibid.payment.infra.dto;

import lombok.Getter;
import lombok.Setter;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmResponse;

@Getter
@Setter
public class PaymentTossDtoImpl implements PaymentConfirmResponse {

	private String version;
	private String paymentKey;
	private String type;
	private String orderId;
	private String orderName;
	private String mId;
	private String currency;
	private String method;
	private Integer totalAmount;
	private Integer balanceAmount;
	private String status;
	private String requestedAt;
	private String approvedAt;
	private boolean useEscrow;
	private String lastTransactionKey;
	private Integer suppliedAmount;
	private Integer vat;
	private boolean cultureExpense;
	private Integer taxFreeAmount;
	private Integer taxExemptionAmount;
	private Cancels[] cancels;
	private boolean isPartialCancelable;
	private Card card;
	private VirtualAccount virtualAccount;
	private String secret;
	private MobilePhone mobilePhone;
	private GiftCertificate giftCertificate;
	private Transfer transfer;
	private Receipt receipt;
	private EasyPay easyPay;
	private String country;
	private Failure failure;
	private CashReceipt cashReceipt;
	private CashReceipts[] cashReceipts;
	private Discount discount;
	private Checkout checkout;
	private Long paymentId;

	private String transactionKey;

	public void setPaymentId(Long paymentId) {
		this.paymentId = paymentId;
	}
}
