package project.kjhjdh.ibid.product.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductLike;
import project.kjhjdh.ibid.product.infra.ProductLikeRepository;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.product.presentation.dto.ProductLikeStatusResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductSummaryResponse;

@Service
@RequiredArgsConstructor
public class ProductLikeService {

    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;
    private final ProductImageService productImageService;

    @Transactional
    public void like(Long userId, Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (productLikeRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }
        productLikeRepository.save(ProductLike.of(userId, productId));
    }

    @Transactional
    public void unlike(Long userId, Long productId) {
        productLikeRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional(readOnly = true)
    public boolean isLiked(Long userId, Long productId) {
        return productLikeRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional(readOnly = true)
    public long countLikes(Long productId) {
        return productLikeRepository.countByProductId(productId);
    }

    @Transactional(readOnly = true)
    public ProductLikeStatusResponse status(Long userId, Long productId) {
        return new ProductLikeStatusResponse(
                productLikeRepository.countByProductId(productId),
                productLikeRepository.existsByUserIdAndProductId(userId, productId)
        );
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> myLikedProducts(Long userId) {
        List<Long> productIds = productLikeRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(ProductLike::getProductId)
                .toList();
        if (productIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        Map<Long, String> thumbnails = productImageService.findThumbnails(productIds);
        return productIds.stream()
                .map(productsById::get)
                .filter(Objects::nonNull)
                .map(product -> ProductSummaryResponse.from(product, thumbnails.get(product.getId())))
                .toList();
    }
}
