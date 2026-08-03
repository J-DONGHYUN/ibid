package project.kjhjdh.ibid.common.image.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class S3ImageUploaderTest {

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3ImageUploader s3ImageUploader;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3ImageUploader, "bucket", "ibid-product-images");
        ReflectionTestUtils.setField(s3ImageUploader, "region", "ap-northeast-2");
    }

    @DisplayName("허용된 확장자면 업로드용/조회용 URL을 발급하고 key를 소문자 확장자로 만든다")
    @Test
    void generatePresignedUrl() throws Exception {
        // given
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        given(presigned.url()).willReturn(URI.create("https://presigned-put-url").toURL());
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presigned);

        // when
        PresignedUploadResult result = s3ImageUploader.generatePresignedUrl("products/1", "photo.JPG", "image/jpeg");

        // then
        assertThat(result.presignedUrl()).isEqualTo("https://presigned-put-url");
        assertThat(result.key()).startsWith("products/1/").endsWith(".jpg");
        assertThat(result.imageUrl())
                .isEqualTo("https://ibid-product-images.s3.ap-northeast-2.amazonaws.com/" + result.key());
    }

    @DisplayName("허용되지 않은 확장자면 예외를 던진다")
    @Test
    void generatePresignedUrl_invalidExtension() {
        // when & then
        assertThatThrownBy(() -> s3ImageUploader.generatePresignedUrl("products/1", "malware.txt", "text/plain"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.FILE_INVALID_EXTENSION.getMessage());
    }

    @DisplayName("확장자가 없으면 예외를 던진다")
    @Test
    void generatePresignedUrl_noExtension() {
        // when & then
        assertThatThrownBy(() -> s3ImageUploader.generatePresignedUrl("products/1", "noext", "image/jpeg"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.FILE_INVALID_EXTENSION.getMessage());
    }
}
