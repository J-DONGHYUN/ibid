package project.kjhjdh.ibid.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequirementCoverageTest {

    private static final Path REQUIREMENTS = Path.of("docs/01-product/requirements.md");
    private static final Path TEST_SOURCE = Path.of("src/test/java");
    private static final Path REPORT = Path.of("build/requirement-coverage.txt");

    private static final Pattern REQUIREMENT_ID = Pattern.compile("\\b(AU|PD|CH|TR|NT)-\\d+\\b");
    private static final Pattern DISPLAY_NAME_ID =
            Pattern.compile("@DisplayName\\(\\s*\"\\[((?:AU|PD|CH|TR|NT|I)-\\d+)]");

    @DisplayName("요구사항 ID 별 테스트 수를 build 리포트로 낸다.")
    @Test
    void reportRequirementCoverage() throws IOException {
        TreeSet<String> requirements = new TreeSet<>(idComparator());
        requirements.addAll(idsMatching(read(REQUIREMENTS), REQUIREMENT_ID));

        Map<String, Integer> testCounts = countDisplayNameIds();

        String report = render(requirements, testCounts);
        Files.createDirectories(REPORT.getParent());
        Files.writeString(REPORT, report);
        System.out.println(report);

        assertThat(Files.readString(REPORT)).isNotBlank();
    }

    private static String render(TreeSet<String> requirements, Map<String, Integer> testCounts) {
        StringBuilder out = new StringBuilder("# 요구사항 커버리지 — @DisplayName ID 별 테스트 수\n\n");

        int covered = 0;
        for (String id : requirements) {
            int count = testCounts.getOrDefault(id, 0);
            if (count > 0) {
                covered++;
            }
            out.append(String.format("[%s] %d%n", id, count));
        }

        int total = requirements.size();
        int percent = total == 0 ? 0 : covered * 100 / total;
        out.append(String.format("%n총 요구사항 %d · 테스트 있는 것 %d (%d%%)%n", total, covered, percent));

        List<String> orphans = testCounts.keySet().stream()
                .filter(id -> !requirements.contains(id))
                .sorted(idComparator())
                .toList();
        if (!orphans.isEmpty()) {
            out.append("\n요구사항에 없는 ID (불변식 I-nn · 오타 확인):\n");
            orphans.forEach(id -> out.append(String.format("[%s] %d%n", id, testCounts.get(id))));
        }
        return out.toString();
    }

    private static Map<String, Integer> countDisplayNameIds() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.walk(TEST_SOURCE)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(RequirementCoverageTest::displayNameIds)
                    .forEach(id -> counts.merge(id, 1, Integer::sum));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return counts;
    }

    private static Stream<String> displayNameIds(Path javaFile) {
        Matcher matcher = DISPLAY_NAME_ID.matcher(read(javaFile));
        Stream.Builder<String> ids = Stream.builder();
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids.build();
    }

    private static List<String> idsMatching(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        Stream.Builder<String> ids = Stream.builder();
        while (matcher.find()) {
            ids.add(matcher.group());
        }
        return ids.build().toList();
    }

    private static Comparator<String> idComparator() {
        return Comparator.comparing((String id) -> id.split("-")[0])
                .thenComparingInt(id -> Integer.parseInt(id.split("-")[1]));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
