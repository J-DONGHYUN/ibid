package project.kjhjdh.ibid.product.presentation;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.resolver.LoginUser;
import project.kjhjdh.ibid.product.application.ProductLikeService;
import project.kjhjdh.ibid.product.presentation.dto.ProductLikeStatusResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductSummaryResponse;
import project.kjhjdh.ibid.product.application.ProductWithThumbnail;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductLikeController {

    private final ProductLikeService productLikeService;

    @GetMapping("/{productId}/like")
    public ResponseEntity<ProductLikeStatusResponse> likeStatus(
            @LoginUser UserInfo loginUser, @PathVariable Long productId) {
        return ResponseEntity.ok(new ProductLikeStatusResponse(
                productLikeService.countLikes(productId),
                productLikeService.isLiked(loginUser.userId(), productId)
        ));
    }

    @PostMapping("/{productId}/like")
    public ResponseEntity<Void> like(@LoginUser UserInfo loginUser, @PathVariable Long productId) {
        productLikeService.like(loginUser.userId(), productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}/like")
    public ResponseEntity<Void> unlike(@LoginUser UserInfo loginUser, @PathVariable Long productId) {
        productLikeService.unlike(loginUser.userId(), productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/likes")
    public ResponseEntity<List<ProductSummaryResponse>> myLikes(@LoginUser UserInfo loginUser) {
        List<ProductSummaryResponse> responses = productLikeService.myLikedProducts(loginUser.userId()).stream()
                .map(item -> ProductSummaryResponse.from(item.product(), item.thumbnailUrl()))
                .toList();
        return ResponseEntity.ok(responses);
    }
}
