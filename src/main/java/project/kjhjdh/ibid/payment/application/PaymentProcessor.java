package project.kjhjdh.ibid.payment.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.payment.domain.Payment;
import project.kjhjdh.ibid.payment.infra.PaymentRepository;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmResponse;

@Component
@RequiredArgsConstructor
public class PaymentProcessor {

    private final PaymentRepository paymentRepository;

    @Transactional
    public void success(Long paymentId, PaymentConfirmResponse response) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.confirm(response.getPaymentKey());

        // TODO:
        //  - order 상태 처리
    }
}
