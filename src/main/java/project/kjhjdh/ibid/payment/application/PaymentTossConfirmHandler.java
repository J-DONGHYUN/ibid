package project.kjhjdh.ibid.payment.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import project.kjhjdh.ibid.payment.domain.TossPayment;
import project.kjhjdh.ibid.payment.domain.TossPaymentMethod;
import project.kjhjdh.ibid.payment.domain.TossPaymentStatus;
import project.kjhjdh.ibid.payment.infra.TossPaymentClient;
import project.kjhjdh.ibid.payment.infra.TossPaymentRepository;
import project.kjhjdh.ibid.payment.infra.dto.PaymentTossDtoImpl;
import project.kjhjdh.ibid.payment.infra.dto.TossConfirmRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmResponse;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTossConfirmHandler {

    private static final String CONFIRM_FAILED_CANCEL_REASON = "결제 승인 실패";

    private final TossPaymentClient tossPaymentClient;
    private final TossPaymentRepository tossPaymentRepository;

    public PaymentConfirmResponse confirm(Long paymentId, TossConfirmRequest request) {
        PaymentTossDtoImpl paymentTossDto = tossPaymentClient.requestConfirm(request);
        try {
            tossPaymentRepository.save(
                    TossPayment.of(
                            paymentTossDto.getPaymentKey(),
                            paymentTossDto.getOrderId(),
                            paymentId,
                            paymentTossDto.getTotalAmount(),
                            TossPaymentMethod.from(paymentTossDto.getMethod()),
                            TossPaymentStatus.valueOf(paymentTossDto.getStatus()),
                            toLocalDateTime(paymentTossDto.getRequestedAt()),
                            toLocalDateTime(paymentTossDto.getApprovedAt())
                    ));
            return paymentTossDto;
        } catch (Exception e) {
            log.error("결제 저장 실패 후 취소 요청도 실패: paymentKey={}", request.paymentKey(), e);
            tossPaymentClient.requestPaymentCancel(request.paymentKey(), CONFIRM_FAILED_CANCEL_REASON);
            throw e;
        }
    }

    private LocalDateTime toLocalDateTime(String offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return OffsetDateTime.parse(offsetDateTime).toLocalDateTime();
    }
}
