package project.kjhjdh.ibid.payment.application;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.domain.OrderStatus;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.payment.domain.Payment;
import project.kjhjdh.ibid.payment.domain.State;
import project.kjhjdh.ibid.payment.infra.PaymentRepository;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmRequest;

@Component
@RequiredArgsConstructor
public class PaymentValidator {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public void validate(Long paymentId, PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        validateOrder(payment.getOrderId());
        validateState(payment);
        validateAmount(payment, request.amount());
    }

    private void validateOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PAYABLE);
        }
    }

    private void validateState(Payment payment) {
        if (payment.getState() != State.READY) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_CONFIRM);
        }
    }

    private void validateAmount(Payment payment, String requestedAmount) {
        long amount = parseAmount(requestedAmount);
        if (payment.getTotalAmount() != amount) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private long parseAmount(String requestedAmount) {
        try {
            return Long.parseLong(requestedAmount);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
