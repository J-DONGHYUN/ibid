package project.kjhjdh.ibid.product.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.image.infra.PresignedUploadResult;
import project.kjhjdh.ibid.product.domain.DeviceSpec;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int PAGE_SIZE = 16;

    private final ProductRepository productRepository;
    private final ProductViewCounter productViewCounter;
    private final ProductImageService productImageService;

    public List<PresignedUploadResult> generatePresignedUrls(Long sellerId, Long productId, List<ImagePresignCommand> commands) {
        findOwnedProduct(sellerId, productId);
        return productImageService.presign(productId, commands);
    }

    @Transactional
    public void confirmImages(Long sellerId, Long productId, List<String> imageUrls) {
        Product product = findOwnedProduct(sellerId, productId);
        productImageService.requireIssued(productId, imageUrls);
        product.addImages(imageUrls);
    }

    @Transactional
    public void update(Long sellerId, Long productId, ProductUpdateCommand command) {
        Product product = findOwnedProduct(sellerId, productId);
        DeviceSpecCommand spec = command.deviceSpec();
        product.update(command.title(), command.description(), command.price(),
                command.productCondition(),
                new DeviceSpec(spec.category(), spec.modelName(), spec.batteryHealth(),
                        spec.components(), spec.defects()));
    }

    @Transactional
    public void deleteImages(Long sellerId, Long productId, List<String> imageUrls) {
        Product product = findOwnedProduct(sellerId, productId);
        List<String> removed = product.removeImages(imageUrls);
        productImageService.deleteFiles(productId, removed);
    }

    @Transactional
    public void delete(Long sellerId, Long productId) {
        Product product = findOwnedProduct(sellerId, productId);
        product.delete();
    }

    @Transactional
    public Long register(Long sellerId, ProductRegisterCommand command) {
        DeviceSpecCommand spec = command.deviceSpec();
        Product product = Product.create(
                sellerId,
                command.title(),
                command.description(),
                command.price(),
                command.productCondition(),
                new DeviceSpec(spec.category(), spec.modelName(), spec.batteryHealth(),
                        spec.components(), spec.defects())
        );
        return productRepository.save(product).getId();
    }

    @Transactional(readOnly = true)
    public ProductListResult getProducts(Long cursor, boolean includeSold) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Long effectiveCursor = (cursor == null) ? Long.MAX_VALUE : cursor;
        Slice<Product> slice = includeSold
                ? productRepository.findByDeletedAtIsNullAndIdLessThanOrderByIdDesc(effectiveCursor, pageable)
                : productRepository.findByDeletedAtIsNullAndStatusNotAndIdLessThanOrderByIdDesc(ProductStatus.SOLD, effectiveCursor, pageable);
        List<Long> productIds = slice.getContent().stream().map(Product::getId).toList();
        Map<Long, String> thumbnails = productImageService.findThumbnails(productIds);
        return new ProductListResult(slice, thumbnails);
    }

    @Transactional(readOnly = true)
    public Map<Long, ProductSummary> findSummaries(List<Long> productIds) {
        Map<Long, String> thumbnails = productImageService.findThumbnails(productIds);
        return productRepository.findAllIncludingDeleted(productIds).stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> new ProductSummary(
                                product.getId(), product.getTitle(), thumbnails.get(product.getId()))));
    }

    @Transactional(readOnly = true)
    public ProductDetailResult getProduct(Long productId, String visitorId) {
        Product product = productRepository.findActiveById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        productViewCounter.record(productId, visitorId);
        return ProductDetailResult.from(product, productViewCounter.readTotal(product));
    }

    private Product findOwnedProduct(Long sellerId, Long productId) {
        Product product = productRepository.findActiveById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (!product.isOwnedBy(sellerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return product;
    }
}
