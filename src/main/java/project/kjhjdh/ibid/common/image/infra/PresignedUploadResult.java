package project.kjhjdh.ibid.common.image.infra;

public record PresignedUploadResult(
        String presignedUrl,
        String key,
        String imageUrl
) {
}
