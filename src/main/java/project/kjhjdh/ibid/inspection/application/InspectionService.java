package project.kjhjdh.ibid.inspection.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.inspection.domain.Inspection;
import project.kjhjdh.ibid.inspection.domain.InspectionFailed;
import project.kjhjdh.ibid.inspection.domain.InspectionPassed;
import project.kjhjdh.ibid.inspection.infra.InspectionRepository;
import project.kjhjdh.ibid.order.application.OrderService;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final OrderService orderService;
    private final InspectionRepository inspectionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void receive(Long orderId) {
        orderService.startInspection(orderId);
    }

    @Transactional
    public void pass(Long inspectorId, Long orderId, String memo) {
        orderService.complete(orderId);
        inspectionRepository.save(Inspection.passed(orderId, inspectorId, memo));
        eventPublisher.publishEvent(new InspectionPassed(orderId));
    }

    @Transactional
    public void fail(Long inspectorId, Long orderId, String memo) {
        orderService.refund(orderId);
        inspectionRepository.save(Inspection.failed(orderId, inspectorId, memo));
        eventPublisher.publishEvent(new InspectionFailed(orderId));
    }
}
