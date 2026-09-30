package project.kjhjdh.ibid.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@AnalyzeClasses(packages = "project.kjhjdh.ibid", importOptions = ImportOption.OnlyIncludeTests.class)
class TestConventionRulesTest {

    private static final Pattern REQUIREMENT_ID = Pattern.compile("^\\[(AU|PD|CH|TR|NT|I)-\\d+\\]");

    @ArchTest
    static final ArchRule TESTS_HAVE_DISPLAY_NAME = FreezingArchRule.freeze(
            methods().that().areAnnotatedWith(Test.class)
                    .should().beAnnotatedWith(DisplayName.class)
                    .because("테스트 이름은 사용자 행위 · 규칙 중심으로 쓴다 (docs/03-convention/test.md 「이름과 구조」)"));

    @ArchTest
    static final ArchRule TEST_DISPLAY_NAMES_START_WITH_REQUIREMENT_ID = FreezingArchRule.freeze(
            methods().that().areAnnotatedWith(Test.class)
                    .and().areDeclaredInClassesThat().resideOutsideOfPackage("..architecture..")
                    .should(startDisplayNameWithRequirementId())
                    .because("검증 기준마다 테스트가 있는지 세려면 기능 테스트의 @DisplayName 이 요구사항 · 불변식 ID 로 "
                            + "시작해야 한다. 하네스 · 아키텍처 자체 테스트는 요구사항과 무관해 제외한다 "
                            + "(docs/03-convention/test.md 「이름과 구조」)"));

    private static ArchCondition<JavaMethod> startDisplayNameWithRequirementId() {
        return new ArchCondition<>("@DisplayName 이 요구사항 · 불변식 ID([TR-01] 같은)로 시작한다") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Optional<DisplayName> displayName = method.tryGetAnnotationOfType(DisplayName.class);
                boolean ok = displayName.isPresent()
                        && REQUIREMENT_ID.matcher(displayName.get().value()).find();
                if (!ok) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " 의 @DisplayName 이 요구사항 · 불변식 ID([AU|PD|CH|TR|NT|I]-nn)로 시작하지 않는다"));
                }
            }
        };
    }

    @ArchTest
    static final ArchRule TEST_CLASSES_ARE_NOT_TRANSACTIONAL =
            noClasses().should().beAnnotatedWith(Transactional.class)
                    .because("롤백 방식이면 동시성 테스트가 성립하지 않는다. DB 정리는 DbCleaner 가 한다 "
                            + "(docs/03-convention/test.md 「계층별」)");

    @ArchTest
    static final ArchRule TEST_METHODS_ARE_NOT_TRANSACTIONAL =
            noMethods().should().beAnnotatedWith(Transactional.class)
                    .because("롤백 방식이면 동시성 테스트가 성립하지 않는다. DB 정리는 DbCleaner 가 한다");

    @ArchTest
    static final ArchRule TESTS_USE_ASSERTJ =
            noClasses().should().dependOnClassesThat().haveFullyQualifiedName("org.junit.jupiter.api.Assertions")
                    .because("검증은 AssertJ 로 하고, 예외는 assertThatThrownBy 로 ErrorCode 까지 본다");
}
