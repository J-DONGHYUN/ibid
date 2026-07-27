package project.kjhjdh.ibid.inspection.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "inspections")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inspection {

    private static final int MEMO_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long inspectorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InspectionResult result;

    @Column(length = MEMO_MAX_LENGTH)
    private String memo;

    private Inspection(Long orderId, Long inspectorId, InspectionResult result, String memo) {
        this.orderId = orderId;
        this.inspectorId = inspectorId;
        this.result = result;
        this.memo = memo;
    }

    public static Inspection passed(Long orderId, Long inspectorId, String memo) {
        return new Inspection(orderId, inspectorId, InspectionResult.PASSED, memo);
    }

    public static Inspection failed(Long orderId, Long inspectorId, String memo) {
        return new Inspection(orderId, inspectorId, InspectionResult.FAILED, memo);
    }

    public boolean isPassed() {
        return result == InspectionResult.PASSED;
    }
}
