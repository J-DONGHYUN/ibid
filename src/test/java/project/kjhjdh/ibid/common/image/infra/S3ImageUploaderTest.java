package project.kjhjdh.ibid.common.image.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.awspring.cloud.s3.S3Template;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class S3ImageUploaderTest {

    private static final String ISSUED_BASE = "https://ibid-product-images.s3.ap-northeast-2.amazonaws.com/";
    private static final String UUID = "0b9f6c1e-2d3a-4f5b-8c7d-9e0a1b2c3d4e";

    @Mock
    private S3Template s3Template;

    @InjectMocks
    private S3ImageUploader s3ImageUploader;

    static Stream<Arguments> notIssuedUrls() {
        return Stream.of(
                Arguments.of("외부 도메인", "https://evil.example.com/tracker.png"),
                Arguments.of("다른 상품 경로", ISSUED_BASE + "products/2/" + UUID + ".png"),
                Arguments.of("다른 버킷", "https://other-bucket.s3.ap-northeast-2.amazonaws.com/products/1/" + UUID + ".png"),
                Arguments.of("경로 탈출", ISSUED_BASE + "products/1/../2/" + UUID + ".png"),
                Arguments.of("하위 경로", ISSUED_BASE + "products/1/sub/" + UUID + ".png"),
                Arguments.of("허용되지 않은 확장자", ISSUED_BASE + "products/1/" + UUID + ".exe"),
                Arguments.of("쿼리스트링 덧붙임", ISSUED_BASE + "products/1/" + UUID + ".png?x=1"),
                Arguments.of("http 주소", "http://ibid-product-images.s3.ap-northeast-2.amazonaws.com/products/1/" + UUID + ".png"),
                Arguments.of("UUID 가 아닌 파일명", ISSUED_BASE + "products/1/photo.png"),
                Arguments.of("대문자 확장자", ISSUED_BASE + "products/1/" + UUID + ".PNG"),
                Arguments.of("접두사만 같은 디렉터리", ISSUED_BASE + "products/10/" + UUID + ".png"));
    }

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

    @DisplayName("[PD-02] 발급 형식과 정확히 일치하는 URL 만 이 디렉터리에서 발급한 것으로 본다")
    @Test
    void isIssuedUrl() {
        // when & then
        assertThat(s3ImageUploader.isIssuedUrl("products/1", ISSUED_BASE + "products/1/" + UUID + ".png")).isTrue();
        assertThat(s3ImageUploader.isIssuedUrl("products/1", ISSUED_BASE + "products/1/" + UUID + ".jpeg")).isTrue();
    }

    @DisplayName("[PD-02] 발급 형식에서 벗어난 URL 은 발급한 것이 아니다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("notIssuedUrls")
    void isIssuedUrl_notIssued(String variant, String url) {
        // when & then
        assertThat(s3ImageUploader.isIssuedUrl("products/1", url)).isFalse();
    }

    @DisplayName("[PD-02] URL 이 null 이면 발급한 것이 아니다")
    @Test
    void isIssuedUrl_null() {
        // when & then
        assertThat(s3ImageUploader.isIssuedUrl("products/1", null)).isFalse();
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
