package project.kjhjdh.ibid.product.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.product.presentation.dto.ProductDetailResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductListResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductRegisterRequest;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int PAGE_SIZE = 16;

    private final ProductRepository productRepository;

    @Transactional
    public Long register(Long sellerId, ProductRegisterRequest request) {
        Product product = Product.create(
                sellerId,
                request.title(),
                request.description(),
                request.price(),
                request.stock()
        );
        return productRepository.save(product).getId();
    }

    @Transactional
    public void openForSale(Long sellerId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isOwnedBy(sellerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        product.openForSale();
    }

    @Transactional(readOnly = true)
    public ProductListResponse getProducts(Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Long effectiveCursor = (cursor == null) ? Long.MAX_VALUE : cursor;
        Slice<Product> slice = productRepository.findByIdLessThanOrderByIdDesc(effectiveCursor, pageable);
        return ProductListResponse.of(slice);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductDetailResponse.from(product);
    }
}
