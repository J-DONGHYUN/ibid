package project.kjhjdh.ibid.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SourceRulesTest {

    private static final Path PRODUCTION_SOURCE = Path.of("src/main/java");

    private static final Pattern COMMENT_LINE = Pattern.compile("^\\s*(//|/\\*)");
    private static final Pattern MUTABLE_LOMBOK = Pattern.compile("^\\s*@(Setter|Data)\\b");

    private static final Set<String> FROZEN_COMMENT_FILES = Set.of(
            "project/kjhjdh/ibid/auth/application/AuthService.java",
            "project/kjhjdh/ibid/auth/infra/RefreshTokenRedisRepository.java",
            "project/kjhjdh/ibid/common/exception/ErrorCode.java",
            "project/kjhjdh/ibid/payment/application/PaymentService.java",
            "project/kjhjdh/ibid/product/presentation/ProductController.java"
    );

    private static final Set<String> FROZEN_MUTABLE_LOMBOK_FILES = Set.of(
            "project/kjhjdh/ibid/payment/infra/dto/PaymentTossDtoImpl.java"
    );

    @DisplayName("프로덕션 코드에 새 주석이 생기지 않는다.")
    @Test
    void productionCode_hasNoNewComments() {
        List<String> violations = filesMatching(COMMENT_LINE).stream()
                .filter(file -> !FROZEN_COMMENT_FILES.contains(file))
                .toList();

        assertThat(violations)
                .as("프로덕션 코드에 주석을 달지 않는다. 의도는 메서드 · 변수 이름으로 드러내고, 이유는 커밋 메시지와 ADR 에 남긴다 "
                        + "(docs/03-convention/code.md 「주석」). 동결 목록에 파일을 더해 통과시키지 않는다")
                .isEmpty();
    }

    @DisplayName("주석을 모두 지운 파일은 동결 목록에서도 지운다.")
    @Test
    void frozenCommentFiles_stillHaveComments() {
        List<String> commentFiles = filesMatching(COMMENT_LINE);
        List<String> resolved = FROZEN_COMMENT_FILES.stream()
                .filter(file -> !commentFiles.contains(file))
                .toList();

        assertThat(resolved)
                .as("이 파일들은 주석이 없어졌다. SourceRulesTest.FROZEN_COMMENT_FILES 에서 지워 동결 목록을 줄인다 (ADR-0010)")
                .isEmpty();
    }

    @DisplayName("프로덕션 코드에 새 @Setter · @Data 가 생기지 않는다.")
    @Test
    void productionCode_hasNoNewMutableLombok() {
        List<String> violations = filesMatching(MUTABLE_LOMBOK).stream()
                .filter(file -> !FROZEN_MUTABLE_LOMBOK_FILES.contains(file))
                .toList();

        assertThat(violations)
                .as("상태는 의미 있는 이름의 메서드로만 바꾼다 (docs/03-convention/code.md 「엔티티」). 동결 목록에 파일을 더해 통과시키지 않는다")
                .isEmpty();
    }

    @DisplayName("@Setter · @Data 를 모두 지운 파일은 동결 목록에서도 지운다.")
    @Test
    void frozenMutableLombokFiles_stillHaveMutableLombok() {
        List<String> mutableLombokFiles = filesMatching(MUTABLE_LOMBOK);
        List<String> resolved = FROZEN_MUTABLE_LOMBOK_FILES.stream()
                .filter(file -> !mutableLombokFiles.contains(file))
                .toList();

        assertThat(resolved)
                .as("이 파일들은 @Setter · @Data 가 없어졌다. SourceRulesTest.FROZEN_MUTABLE_LOMBOK_FILES 에서 지워 동결 목록을 줄인다 (ADR-0010)")
                .isEmpty();
    }

    private static List<String> filesMatching(Pattern pattern) {
        try (Stream<Path> paths = Files.walk(PRODUCTION_SOURCE)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> anyLineMatches(path, pattern))
                    .map(path -> PRODUCTION_SOURCE.relativize(path).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean anyLineMatches(Path path, Pattern pattern) {
        try (Stream<String> lines = Files.lines(path)) {
            return lines.anyMatch(line -> pattern.matcher(line).find());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
