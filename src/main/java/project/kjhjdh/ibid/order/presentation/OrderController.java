package project.kjhjdh.ibid.order.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.resolver.LoginUser;
import project.kjhjdh.ibid.order.application.OrderService;
import project.kjhjdh.ibid.order.application.PurchaseResult;
import project.kjhjdh.ibid.order.presentation.dto.MyOrdersResponse;
import project.kjhjdh.ibid.order.presentation.dto.MyTransactionsResponse;
import project.kjhjdh.ibid.order.presentation.dto.OrderDetailResponse;
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
        PurchaseResult result = orderService.purchase(loginUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PurchaseResponse(result.orderId()));
    }

    @PostMapping("/{orderId}/ship")
    public ResponseEntity<Void> ship(
            @LoginUser UserInfo loginUser,
            @PathVariable Long orderId
    ) {
        orderService.ship(loginUser.userId(), orderId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<MyOrdersResponse> getMyOrders(
            @LoginUser UserInfo loginUser,
            @RequestParam String role
    ) {
        return ResponseEntity.ok(orderService.getMyOrders(loginUser.userId(), role));
    }

    @GetMapping("/me")
    public ResponseEntity<MyTransactionsResponse> getMyTransactions(@LoginUser UserInfo loginUser) {
        return ResponseEntity.ok(orderService.getMyTransactions(loginUser.userId()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getMyOrder(
            @LoginUser UserInfo loginUser,
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(orderService.getMyOrder(loginUser.userId(), orderId));
    }
}
