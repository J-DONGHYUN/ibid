package project.kjhjdh.ibid.product.presentation;

import static project.kjhjdh.ibid.product.presentation.cookie.ProductViewCookieHandler.VISITOR_ID_COOKIE_NAME;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.interceptor.PublicApi;
import project.kjhjdh.ibid.auth.presentation.resolver.LoginUser;
import project.kjhjdh.ibid.product.application.ProductService;
import project.kjhjdh.ibid.product.presentation.cookie.ProductViewCookieHandler;
import project.kjhjdh.ibid.product.presentation.dto.ProductDetailResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductListResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductRegisterRequest;
import project.kjhjdh.ibid.product.presentation.dto.ProductRegisterResponse;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductViewCookieHandler productViewCookieHandler;

    @PostMapping
    public ResponseEntity<ProductRegisterResponse> register(
            @LoginUser UserInfo loginUser,
            @Valid @RequestBody ProductRegisterRequest request
    ) {
        Long productId = productService.register(loginUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ProductRegisterResponse(productId));
    }

    @PublicApi
    @GetMapping
    public ResponseEntity<ProductListResponse> getProducts(
            @RequestParam(required = false) Long cursor
    ) {
        return ResponseEntity.ok(productService.getProducts(cursor));
    }

    @PublicApi
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProduct(
            @PathVariable Long productId,
            @CookieValue(name = VISITOR_ID_COOKIE_NAME, required = false) String visitorId
    ) {
        String resolvedVisitorId = productViewCookieHandler.resolveVisitorId(visitorId);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        productViewCookieHandler.createVisitorIdCookie(resolvedVisitorId).toString())
                .body(productService.getProduct(productId, resolvedVisitorId));
    }

    @PatchMapping("/{productId}/on-sale")
    public ResponseEntity<Void> openForSale(
            @LoginUser UserInfo loginUser,
            @PathVariable Long productId
    ) {
        productService.openForSale(loginUser.userId(), productId);
        return ResponseEntity.ok().build();
    }
}
