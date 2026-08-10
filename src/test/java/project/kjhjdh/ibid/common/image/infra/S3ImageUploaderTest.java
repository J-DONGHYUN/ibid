package project.kjhjdh.ibid.common.image.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.awspring.cloud.s3.S3Template;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class S3ImageUploaderTest {

    @Mock
    private S3Template s3Template;

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
        given(s3Template.createSignedPutURL(any(), any(), any()))
                .willReturn(URI.create("https://presigned-put-url").toURL());

        // when
        PresignedUploadResult result = s3ImageUploader.generatePresignedUrl("products/1", "photo.JPG");

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
        assertThatThrownBy(() -> s3ImageUploader.generatePresignedUrl("products/1", "malware.txt"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.FILE_INVALID_EXTENSION.getMessage());
    }

    @DisplayName("확장자가 없으면 예외를 던진다")
    @Test
    void generatePresignedUrl_noExtension() {
        // when & then
        assertThatThrownBy(() -> s3ImageUploader.generatePresignedUrl("products/1", "noext"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.FILE_INVALID_EXTENSION.getMessage());
    }

    @DisplayName("이미지 URL에서 key를 추출해 S3에서 삭제한다")
    @Test
    void delete() {
        // when
        s3ImageUploader.delete("https://ibid-product-images.s3.ap-northeast-2.amazonaws.com/products/1/uuid.jpg");

        // then
        org.mockito.BDDMockito.then(s3Template).should()
                .deleteObject("ibid-product-images", "products/1/uuid.jpg");
    }
}
