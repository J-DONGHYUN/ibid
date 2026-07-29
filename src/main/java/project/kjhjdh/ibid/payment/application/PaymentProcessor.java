package project.kjhjdh.ibid.payment.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.payment.domain.Payment;
import project.kjhjdh.ibid.payment.infra.PaymentRepository;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmResponse;
import project.kjhjdh.ibid.product.application.ProductStockHandler;

@Component
@RequiredArgsConstructor
public class PaymentProcessor {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductStockHandler productStockHandler;

    @Transactional
    public void success(Long paymentId, PaymentConfirmResponse response) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.confirm(response.getPaymentKey());

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.paid();

        productStockHandler.decreaseStock(order.getProductId(), order.getQuantity());
    }
}
