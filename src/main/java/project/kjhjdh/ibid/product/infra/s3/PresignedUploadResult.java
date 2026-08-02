package project.kjhjdh.ibid.product.infra.s3;

public record PresignedUploadResult(
        String presignedUrl,
        String key,
        String imageUrl
) {
}
