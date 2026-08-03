package project.kjhjdh.ibid.common.image.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.kjhjdh.ibid.common.image.domain.Image;
import project.kjhjdh.ibid.common.image.domain.ImageOwnerType;
import project.kjhjdh.ibid.common.image.infra.ImageRepository;
import project.kjhjdh.ibid.common.image.infra.S3ImageUploader;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    private static final Long OWNER_ID = 1L;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private S3ImageUploader s3ImageUploader;

    @InjectMocks
    private ImageService imageService;

    @Captor
    private ArgumentCaptor<List<Image>> imagesCaptor;

    @DisplayName("기존 이미지 뒤에 이어서 sortOrder를 매겨 저장한다")
    @Test
    void attach() {
        // given
        given(imageRepository.countByOwnerTypeAndOwnerId(ImageOwnerType.PRODUCT, OWNER_ID)).willReturn(2);

        // when
        imageService.attach(ImageOwnerType.PRODUCT, OWNER_ID,
                List.of("https://img/a.jpg", "https://img/b.png"));

        // then
        then(imageRepository).should().saveAll(imagesCaptor.capture());
        List<Image> saved = imagesCaptor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getSortOrder()).isEqualTo(2);
        assertThat(saved.get(1).getSortOrder()).isEqualTo(3);
    }

    @DisplayName("소유자별 첫 이미지를 대표 이미지로 매핑한다")
    @Test
    void findThumbnails() {
        // given
        given(imageRepository.findByOwnerTypeAndOwnerIdInOrderBySortOrder(ImageOwnerType.PRODUCT, List.of(1L, 2L)))
                .willReturn(List.of(
                        Image.of(ImageOwnerType.PRODUCT, 1L, "https://img/1a.jpg", 0),
                        Image.of(ImageOwnerType.PRODUCT, 1L, "https://img/1b.jpg", 1),
                        Image.of(ImageOwnerType.PRODUCT, 2L, "https://img/2a.png", 0)
                ));

        // when
        Map<Long, String> thumbnails = imageService.findThumbnails(ImageOwnerType.PRODUCT, List.of(1L, 2L));

        // then
        assertThat(thumbnails).containsEntry(1L, "https://img/1a.jpg");
        assertThat(thumbnails).containsEntry(2L, "https://img/2a.png");
    }

    @DisplayName("빈 소유자 목록이면 조회 없이 빈 맵을 반환한다")
    @Test
    void findThumbnails_empty() {
        // when
        Map<Long, String> thumbnails = imageService.findThumbnails(ImageOwnerType.PRODUCT, List.of());

        // then
        assertThat(thumbnails).isEmpty();
    }

    @DisplayName("선택한 이미지를 DB와 S3에서 함께 삭제한다")
    @Test
    void deleteByUrls() {
        // given
        List<Image> images = List.of(
                Image.of(ImageOwnerType.PRODUCT, OWNER_ID, "https://img/a.jpg", 0),
                Image.of(ImageOwnerType.PRODUCT, OWNER_ID, "https://img/b.jpg", 1)
        );
        given(imageRepository.findByOwnerTypeAndOwnerIdAndUrlIn(ImageOwnerType.PRODUCT, OWNER_ID,
                List.of("https://img/a.jpg", "https://img/b.jpg"))).willReturn(images);

        // when
        imageService.deleteByUrls(ImageOwnerType.PRODUCT, OWNER_ID, List.of("https://img/a.jpg", "https://img/b.jpg"));

        // then
        then(imageRepository).should().deleteAll(images);
        then(s3ImageUploader).should(times(2)).delete(anyString());
    }

    @DisplayName("소유자의 모든 이미지를 DB와 S3에서 삭제한다")
    @Test
    void deleteAll() {
        // given
        List<Image> images = List.of(
                Image.of(ImageOwnerType.PRODUCT, OWNER_ID, "https://img/a.jpg", 0),
                Image.of(ImageOwnerType.PRODUCT, OWNER_ID, "https://img/b.jpg", 1)
        );
        given(imageRepository.findByOwnerTypeAndOwnerId(ImageOwnerType.PRODUCT, OWNER_ID)).willReturn(images);

        // when
        imageService.deleteAll(ImageOwnerType.PRODUCT, OWNER_ID);

        // then
        then(imageRepository).should().deleteAll(images);
        then(s3ImageUploader).should(times(2)).delete(anyString());
    }
}
