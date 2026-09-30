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
import project.kjhjdh.ibid.common.image.infra.PresignedUploadResult;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.Tag;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.product.infra.TagRepository;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int PAGE_SIZE = 16;

    private final ProductRepository productRepository;
    private final TagRepository tagRepository;
    private final ProductViewCounter productViewCounter;
    private final ProductImageService productImageService;

    public List<PresignedUploadResult> generatePresignedUrls(Long sellerId, Long productId, List<ImagePresignCommand> commands) {
        findOwnedProduct(sellerId, productId);
        return productImageService.presign(productId, commands);
    }

    @Transactional
    public void confirmImages(Long sellerId, Long productId, List<String> imageUrls) {
        Product product = findOwnedProduct(sellerId, productId);
        product.addImages(imageUrls);
    }

    @Transactional
    public void update(Long sellerId, Long productId, ProductUpdateCommand command) {
        Product product = findOwnedProduct(sellerId, productId);
        product.update(command.title(), command.description(), command.price(),
                command.productCondition(), resolveTags(command.tags()));
    }

    @Transactional
    public void deleteImages(Long sellerId, Long productId, List<String> imageUrls) {
        Product product = findOwnedProduct(sellerId, productId);
        List<String> removed = product.removeImages(imageUrls);
        productImageService.deleteFiles(removed);
    }

    @Transactional
    public void delete(Long sellerId, Long productId) {
        Product product = findOwnedProduct(sellerId, productId);
        List<String> imageUrls = product.imageUrls();
        productRepository.delete(product);
        productImageService.deleteFiles(imageUrls);
    }

    @Transactional
    public Long register(Long sellerId, ProductRegisterCommand command) {
        Product product = Product.create(
                sellerId,
                command.title(),
                command.description(),
                command.price(),
                command.productCondition(),
                resolveTags(command.tags())
        );
        return productRepository.save(product).getId();
    }

    @Transactional(readOnly = true)
    public ProductListResult getProducts(Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Long effectiveCursor = (cursor == null) ? Long.MAX_VALUE : cursor;
        Slice<Product> slice = productRepository.findByIdLessThanOrderByIdDesc(effectiveCursor, pageable);
        List<Long> productIds = slice.getContent().stream().map(Product::getId).toList();
        Map<Long, String> thumbnails = productImageService.findThumbnails(productIds);
        return new ProductListResult(slice, thumbnails);
    }

    @Transactional(readOnly = true)
    public ProductDetailResult getProduct(Long productId, String visitorId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        productViewCounter.record(productId, visitorId);
        return ProductDetailResult.from(product, productViewCounter.readTotal(product));
    }

    private List<Tag> resolveTags(List<String> names) {
        if (names == null) {
            return List.of();
        }
        return names.stream()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .map(name -> tagRepository.findByName(name)
                        .orElseGet(() -> tagRepository.save(Tag.of(name))))
                .toList();
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
