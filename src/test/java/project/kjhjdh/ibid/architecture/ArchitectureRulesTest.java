package project.kjhjdh.ibid.architecture;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import project.kjhjdh.ibid.product.infra.ProductRepository;

@AnalyzeClasses(packages = "project.kjhjdh.ibid", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule DOMAIN_DEPENDENCY_DIRECTION = FreezingArchRule.freeze(
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .withOptionalLayers(true)
                    .layer("common").definedBy("project.kjhjdh.ibid.common..")
                    .layer("user").definedBy("project.kjhjdh.ibid.user..")
                    .layer("auth").definedBy("project.kjhjdh.ibid.auth..")
                    .layer("product").definedBy("project.kjhjdh.ibid.product..")
                    .layer("chat").definedBy("project.kjhjdh.ibid.chat..")
                    .layer("trade").definedBy("project.kjhjdh.ibid.trade..")
                    .layer("notification").definedBy("project.kjhjdh.ibid.notification..")
                    .layer("legacy").definedBy(
                            "project.kjhjdh.ibid.order..",
                            "project.kjhjdh.ibid.payment..",
                            "project.kjhjdh.ibid.inspection..")
                    .whereLayer("common").mayNotAccessAnyLayer()
                    .whereLayer("user").mayOnlyAccessLayers("common")
                    .whereLayer("auth").mayOnlyAccessLayers("user", "common")
                    .whereLayer("product").mayOnlyAccessLayers("auth", "common")
                    .whereLayer("chat").mayOnlyAccessLayers("product", "auth", "common")
                    .whereLayer("trade").mayOnlyAccessLayers("chat", "product", "auth", "common")
                    .whereLayer("notification").mayOnlyAccessLayers("auth", "common")
                    .whereLayer("notification").mayNotBeAccessedByAnyLayer()
                    .whereLayer("legacy").mayNotBeAccessedByAnyLayer()
                    .because("도메인 의존 방향은 docs/03-convention/architecture.md 「도메인과 의존 방향」 표가 정본이다. "
                            + "product 가 chat 을 보면 순환이다 — 둘을 모두 봐야 하면 trade 에 둔다 (ADR-0006). "
                            + "order · payment · inspection 은 삭제 예정이라 새 코드가 의존하지 않는다"));

    @ArchTest
    static final ArchRule APPLICATION_DOES_NOT_DEPEND_ON_PRESENTATION = FreezingArchRule.freeze(
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..presentation..")
                    .because("서비스는 요청 DTO 대신 application 에 둔 커맨드(~Command record)를 받는다 "
                            + "(docs/03-convention/code.md 「DTO · 커맨드」)"));

    @ArchTest
    static final ArchRule DOMAIN_IS_FRAMEWORK_FREE =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("domain 은 스프링 없이 단위 테스트한다. common.exception 과 jakarta.persistence 만 허용한다");

    @ArchTest
    static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infra..", "..presentation..")
                    .because("domain 은 의존 그래프의 끝이다 (docs/03-convention/architecture.md 「계층」)");

    @ArchTest
    static final ArchRule TRANSACTIONAL_CLASSES_ONLY_IN_APPLICATION =
            noClasses().that().resideOutsideOfPackage("..application..")
                    .should().beAnnotatedWith(Transactional.class)
                    .because("트랜잭션 경계는 application 의 public 메서드에 둔다");

    @ArchTest
    static final ArchRule TRANSACTIONAL_METHODS_ONLY_IN_APPLICATION = FreezingArchRule.freeze(
            noMethods().that().areDeclaredInClassesThat().resideOutsideOfPackage("..application..")
                    .should().beAnnotatedWith(Transactional.class)
                    .because("트랜잭션 경계는 application 의 public 메서드에 둔다. 저장소 메서드에 두지 않는다"));

    @ArchTest
    static final ArchRule REPOSITORIES_LIVE_IN_INFRA =
            classes().that().areInterfaces().and().areAssignableTo(Repository.class)
                    .should().resideInAPackage("..infra..")
                    .because("저장소 인터페이스는 infra 에 두고 Spring Data 를 상속한다 (ADR-0001)");

    @ArchTest
    static final ArchRule PRODUCT_IS_READ_BY_INTENT_NOT_BY_RAW_ID = FreezingArchRule.freeze(
            noClasses().that().resideInAPackage("..application..")
                    .should().callMethodWhere(
                            target(nameMatching("findById|existsById|findAllById"))
                                    .and(target(owner(assignableTo(ProductRepository.class)))))
                    .because("삭제된 상품에 쓰기가 새지 않도록 상품은 용도에 맞는 조회로 읽는다 — 쓰기 경로는 findActiveById, "
                            + "삭제를 포함해 읽어야 하면 findIncludingDeleted (docs/02-design/adr/ADR-0013-deleted-product-writes.md, I-13)"));
}
