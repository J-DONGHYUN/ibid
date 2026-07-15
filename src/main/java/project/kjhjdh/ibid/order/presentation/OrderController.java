package project.kjhjdh.ibid.order.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.resolver.LoginUser;
import project.kjhjdh.ibid.order.application.OrderService;
import project.kjhjdh.ibid.order.presentation.dto.PurchaseRequest;
import project.kjhjdh.ibid.order.presentation.dto.PurchaseResponse;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<PurchaseResponse> purchase(
            @LoginUser UserInfo loginUser,
            @Valid @RequestBody PurchaseRequest request
    ) {
        Long orderId = orderService.purchase(loginUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PurchaseResponse(orderId));
    }
}
