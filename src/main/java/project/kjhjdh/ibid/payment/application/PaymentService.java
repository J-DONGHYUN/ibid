package project.kjhjdh.ibid.payment.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.payment.domain.Payment;
import project.kjhjdh.ibid.payment.infra.PaymentRepository;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmResponse;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateResponse;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentTossConfirmHandler paymentTossConfirmHandler;
    private final PaymentProcessor paymentProcessor;

    @Transactional
    public PaymentCreateResponse create(PaymentCreateRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        Payment payment = paymentRepository.save(Payment.ready(request.orderId(), order.getTotalPrice()));
        return new PaymentCreateResponse(payment.getId());
    }

    public PaymentConfirmResponse confirm(Long paymentId, PaymentConfirmRequest request) {
        // TODO:
        //   - 주문: orderId + CREATED 로 조회, 없으면 NOT_FOUND_DATA
        //   - 결제: state != READY                → PAYMENT_INVALID_STATE
        //   - payment.paidAmount != 요청 amount   → PAYMENT_AMOUNT_MISMATCH (금액 위변조 방어)

        PaymentConfirmResponse confirm = paymentTossConfirmHandler.confirm(paymentId, request.toTossConfirmRequest());

        paymentProcessor.success(paymentId, confirm);

        // TODO: 재고 감소 로직
        return confirm;
    }

}
