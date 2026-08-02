package project.kjhjdh.ibid.product.application;

import java.util.List;

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
import project.kjhjdh.ibid.product.infra.s3.PresignedUploadResult;
import project.kjhjdh.ibid.product.infra.s3.S3ImageUploader;
import project.kjhjdh.ibid.product.presentation.dto.ImageConfirmRequest;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignRequest;
import project.kjhjdh.ibid.product.presentation.dto.ImagePresignResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductDetailResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductListResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductRegisterRequest;
import project.kjhjdh.ibid.product.presentation.dto.ProductUpdateRequest;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int PAGE_SIZE = 16;

    private final ProductRepository productRepository;
    private final S3ImageUploader s3ImageUploader;

    public List<ImagePresignResponse> generatePresignedUrls(Long sellerId, Long productId, List<ImagePresignRequest> requests) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isOwnedBy(sellerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return requests.stream()
                .map(req -> {
                    PresignedUploadResult result = s3ImageUploader.generatePresignedUrl(
                            "products/" + productId, req.filename(), req.contentType());
                    return new ImagePresignResponse(result.presignedUrl(), result.key(), result.imageUrl());
                })
                .toList();
    }

    @Transactional
    public void confirmImages(Long sellerId, Long productId, ImageConfirmRequest request) {
        Product product = findOwnedProduct(sellerId, productId);
        product.addImageUrls(request.imageUrls());
    }

    @Transactional
    public void update(Long sellerId, Long productId, ProductUpdateRequest request) {
        Product product = findOwnedProduct(sellerId, productId);
        product.update(request.title(), request.description(), request.price(), request.stock(), request.condition());
    }

    @Transactional
    public void deleteImages(Long sellerId, Long productId, List<String> imageUrls) {
        Product product = findOwnedProduct(sellerId, productId);
        product.removeImageUrls(imageUrls);
        imageUrls.forEach(s3ImageUploader::delete);
    }

    @Transactional
    public void delete(Long sellerId, Long productId) {
        Product product = findOwnedProduct(sellerId, productId);
        product.validateModifiable();
        List<String> images = List.copyOf(product.getImageUrls());
        productRepository.delete(product);
        images.forEach(s3ImageUploader::delete);
    }

    private Product findOwnedProduct(Long sellerId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isOwnedBy(sellerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return product;
    }

    @Transactional
    public Long register(Long sellerId, ProductRegisterRequest request) {
        Product product = Product.create(
                sellerId,
                request.title(),
                request.description(),
                request.price(),
                request.stock(),
                request.condition()
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
