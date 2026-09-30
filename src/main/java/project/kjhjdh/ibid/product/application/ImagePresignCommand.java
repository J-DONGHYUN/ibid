package project.kjhjdh.ibid.product.application;

public record ImagePresignCommand(
        String filename,
        String contentType
) {
}
