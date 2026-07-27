package project.kjhjdh.ibid.inspection.infra;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import project.kjhjdh.ibid.inspection.domain.Inspection;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    Optional<Inspection> findByOrderId(Long orderId);
}
