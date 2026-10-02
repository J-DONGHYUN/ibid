package project.kjhjdh.ibid.trade.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.resolver.LoginUser;
import project.kjhjdh.ibid.product.application.ProductListResult;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.presentation.dto.ProductListResponse;
import project.kjhjdh.ibid.trade.application.TradeHistoryService;

@RestController
@RequiredArgsConstructor
public class TradeHistoryController {

    private final TradeHistoryService tradeHistoryService;

    @GetMapping("/api/products/me/sales")
    public ResponseEntity<ProductListResponse> sales(
            @LoginUser UserInfo loginUser,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Long cursor
    ) {
        ProductListResult result = tradeHistoryService.getSales(loginUser.userId(), status, cursor);
        return ResponseEntity.ok(ProductListResponse.of(result.slice(), result.thumbnails()));
    }

    @GetMapping("/api/products/me/purchases")
    public ResponseEntity<ProductListResponse> purchases(
            @LoginUser UserInfo loginUser,
            @RequestParam(required = false) Long cursor
    ) {
        ProductListResult result = tradeHistoryService.getPurchases(loginUser.userId(), cursor);
        return ResponseEntity.ok(ProductListResponse.of(result.slice(), result.thumbnails()));
    }
}
