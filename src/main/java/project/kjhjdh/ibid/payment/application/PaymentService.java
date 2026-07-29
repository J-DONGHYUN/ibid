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
    private final PaymentValidator paymentValidator;

    @Transactional
    public PaymentCreateResponse create(PaymentCreateRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        Payment payment = paymentRepository.save(Payment.ready(request.orderId(), order.getTotalPrice()));
        return new PaymentCreateResponse(payment.getId());
    }

    public PaymentConfirmResponse confirm(Long paymentId, PaymentConfirmRequest request) {
        paymentValidator.validate(paymentId, request);

        PaymentConfirmResponse confirm = paymentTossConfirmHandler.confirm(paymentId, request.toTossConfirmRequest());

        paymentProcessor.success(paymentId, confirm);

        return confirm;
    }

}
