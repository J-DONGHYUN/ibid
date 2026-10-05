package project.kjhjdh.ibid.product.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.common.image.infra.PresignedUploadResult;
import project.kjhjdh.ibid.common.image.infra.S3ImageUploader;
import project.kjhjdh.ibid.product.domain.ProductImage;
import project.kjhjdh.ibid.product.infra.ProductImageRepository;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private static final String DIRECTORY_PREFIX = "products/";

    private final ProductImageRepository productImageRepository;
    private final S3ImageUploader s3ImageUploader;

    public List<PresignedUploadResult> presign(Long productId, List<ImagePresignCommand> commands) {
        String directory = directoryOf(productId);
        return commands.stream()
                .map(command -> s3ImageUploader.generatePresignedUrl(directory, command.filename()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findThumbnails(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productImageRepository.findByProductIdInOrderBySortOrder(productIds).stream()
                .collect(Collectors.toMap(image -> image.getProduct().getId(), ProductImage::getUrl, (first, second) -> first));
    }

    public void requireIssued(Long productId, List<String> urls) {
        String directory = directoryOf(productId);
        if (!urls.stream().allMatch(url -> s3ImageUploader.isIssuedUrl(directory, url))) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_URL);
        }
    }

    public void deleteFiles(Long productId, List<String> urls) {
        String directory = directoryOf(productId);
        urls.stream()
                .filter(url -> s3ImageUploader.isIssuedUrl(directory, url))
                .forEach(s3ImageUploader::delete);
    }

    private String directoryOf(Long productId) {
        return DIRECTORY_PREFIX + productId;
    }
}
