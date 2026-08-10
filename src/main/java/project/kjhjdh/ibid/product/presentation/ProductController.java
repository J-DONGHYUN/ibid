package project.kjhjdh.ibid.product.presentation;

import static project.kjhjdh.ibid.product.presentation.cookie.ProductViewCookieHandler.VISITOR_ID_COOKIE_NAME;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import project.kjhjdh.ibid.product.presentation.dto.ImageConfirmRequest;
import project.kjhjdh.ibid.product.presentation.dto.ImageDeleteRequest;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignRequest;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductDetailResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductListResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductRegisterRequest;
import project.kjhjdh.ibid.product.presentation.dto.ProductRegisterResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductUpdateRequest;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductViewCookieHandler productViewCookieHandler;

    // STEP 1: presigned URL 발급 (파일은 서버 안 거침)
    @PostMapping("/{productId}/images/presign")
    public ResponseEntity<List<ImagePresignResponse>> presignImages(
            @LoginUser UserInfo loginUser,
            @PathVariable Long productId,
            @Valid @RequestBody List<ImagePresignRequest> requests
    ) {
        return ResponseEntity.ok(productService.generatePresignedUrls(loginUser.userId(), productId, requests));
    }

    // STEP 2: 브라우저가 S3에 직접 업로드 완료 후 URL 확정 저장
    @PostMapping("/{productId}/images/confirm")
    public ResponseEntity<Void> confirmImages(
            @LoginUser UserInfo loginUser,
            @PathVariable Long productId,
            @RequestBody ImageConfirmRequest request
    ) {
        productService.confirmImages(loginUser.userId(), productId, request);
        return ResponseEntity.ok().build();
    }

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

    @PatchMapping("/{productId}")
    public ResponseEntity<Void> update(
            @LoginUser UserInfo loginUser,
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        productService.update(loginUser.userId(), productId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(
            @LoginUser UserInfo loginUser,
            @PathVariable Long productId
    ) {
        productService.delete(loginUser.userId(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}/images")
    public ResponseEntity<Void> deleteImages(
            @LoginUser UserInfo loginUser,
            @PathVariable Long productId,
            @RequestBody ImageDeleteRequest request
    ) {
        productService.deleteImages(loginUser.userId(), productId, request.imageUrls());
        return ResponseEntity.ok().build();
    }
}
