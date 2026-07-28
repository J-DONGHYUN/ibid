package project.kjhjdh.ibid.payment.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.payment.domain.Payment;
import project.kjhjdh.ibid.payment.domain.TossPayment;
import project.kjhjdh.ibid.payment.domain.TossPaymentMethod;
import project.kjhjdh.ibid.payment.domain.TossPaymentStatus;
import project.kjhjdh.ibid.payment.infra.PaymentRepository;
import project.kjhjdh.ibid.payment.infra.TossPaymentClient;
import project.kjhjdh.ibid.payment.infra.TossPaymentRepository;
import project.kjhjdh.ibid.payment.infra.dto.PaymentTossDtoImpl;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmResponse;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateResponse;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String CONFIRM_FAILED_CANCEL_REASON = "결제 승인 실패";

    private final TossPaymentClient tossPaymentClient;
    private final TossPaymentRepository tossPaymentRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentCreateResponse create(PaymentCreateRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        Payment payment = paymentRepository.save(Payment.ready(request.orderId(), order.getTotalPrice()));
        return new PaymentCreateResponse(payment.getId());
    }

    @Transactional
    public PaymentConfirmResponse confirm(Long paymentId, PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        PaymentTossDtoImpl paymentTossDto = tossPaymentClient.requestConfirm(request.toTossConfirmRequest());
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
            payment.confirm(paymentTossDto.getPaymentKey());

            return paymentTossDto;
        } catch (Exception e) {
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
