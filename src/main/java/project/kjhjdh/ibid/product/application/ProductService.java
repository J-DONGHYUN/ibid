package project.kjhjdh.ibid.product.application;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.image.application.ImageService;
import project.kjhjdh.ibid.common.image.application.PresignTarget;
import project.kjhjdh.ibid.common.image.application.PresignedImage;
import project.kjhjdh.ibid.common.image.domain.ImageOwnerType;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;
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
    private final ImageService imageService;

    public List<ImagePresignResponse> generatePresignedUrls(Long sellerId, Long productId, List<ImagePresignRequest> requests) {
        findOwnedProduct(sellerId, productId);
        List<PresignTarget> targets = requests.stream()
                .map(req -> new PresignTarget(req.filename(), req.contentType()))
                .toList();
        return imageService.presign(ImageOwnerType.PRODUCT, productId, targets).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void confirmImages(Long sellerId, Long productId, ImageConfirmRequest request) {
        findOwnedProduct(sellerId, productId);
        imageService.attach(ImageOwnerType.PRODUCT, productId, request.imageUrls());
    }

    @Transactional
    public void update(Long sellerId, Long productId, ProductUpdateRequest request) {
        Product product = findOwnedProduct(sellerId, productId);
        product.update(request.title(), request.description(), request.price(), request.stock(), request.condition());
    }

    @Transactional
    public void deleteImages(Long sellerId, Long productId, List<String> imageUrls) {
        findOwnedProduct(sellerId, productId);
        imageService.deleteByUrls(ImageOwnerType.PRODUCT, productId, imageUrls);
    }

    @Transactional
    public void delete(Long sellerId, Long productId) {
        Product product = findOwnedProduct(sellerId, productId);
        product.validateModifiable();
        productRepository.delete(product);
        imageService.deleteAll(ImageOwnerType.PRODUCT, productId);
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
        Product product = findOwnedProduct(sellerId, productId);
        product.openForSale();
    }

    @Transactional(readOnly = true)
    public ProductListResponse getProducts(Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Long effectiveCursor = (cursor == null) ? Long.MAX_VALUE : cursor;
        Slice<Product> slice = productRepository.findByIdLessThanOrderByIdDesc(effectiveCursor, pageable);
        List<Long> productIds = slice.getContent().stream().map(Product::getId).toList();
        Map<Long, String> thumbnails = imageService.findThumbnails(ImageOwnerType.PRODUCT, productIds);
        return ProductListResponse.of(slice, thumbnails);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        List<String> imageUrls = imageService.findUrls(ImageOwnerType.PRODUCT, productId);
        return ProductDetailResponse.from(product, imageUrls);
    }

    private ImagePresignResponse toResponse(PresignedImage presigned) {
        return new ImagePresignResponse(presigned.presignedUrl(), presigned.key(), presigned.imageUrl());
    }

    private Product findOwnedProduct(Long sellerId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isOwnedBy(sellerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return product;
    }
}
