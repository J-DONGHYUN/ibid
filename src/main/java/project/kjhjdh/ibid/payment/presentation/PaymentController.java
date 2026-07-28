package project.kjhjdh.ibid.payment.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.kjhjdh.ibid.payment.application.PaymentService;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentConfirmResponse;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateRequest;
import project.kjhjdh.ibid.payment.presentation.dto.PaymentCreateResponse;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping
	public ResponseEntity<PaymentCreateResponse> create(@Valid @RequestBody PaymentCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(request));
	}

	@PostMapping("/{paymentId}/confirm")
	public ResponseEntity<PaymentConfirmResponse> confirm(
		@PathVariable Long paymentId,
		@Valid @RequestBody PaymentConfirmRequest request
	) {
		return ResponseEntity.ok(paymentService.confirm(paymentId, request));
	}
}
