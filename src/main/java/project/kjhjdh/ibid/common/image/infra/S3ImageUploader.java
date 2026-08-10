package project.kjhjdh.ibid.common.image.infra;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

@Component
@RequiredArgsConstructor
public class S3ImageUploader {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif");
    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(5);

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    public PresignedUploadResult generatePresignedUrl(String directory, String originalFilename) {
        String ext = extractExtension(originalFilename);
        String key = directory + "/" + UUID.randomUUID() + "." + ext;

        String presignedUrl = s3Template.createSignedPutURL(bucket, key, PRESIGN_DURATION).toString();
        String imageUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        return new PresignedUploadResult(presignedUrl, key, imageUrl);
    }

    public void delete(String imageUrl) {
        String key = imageUrl.substring(imageUrl.indexOf(".amazonaws.com/") + ".amazonaws.com/".length());
        s3Template.deleteObject(bucket, key);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException(ErrorCode.FILE_INVALID_EXTENSION);
        }
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.FILE_INVALID_EXTENSION);
        }
        return ext;
    }
}
