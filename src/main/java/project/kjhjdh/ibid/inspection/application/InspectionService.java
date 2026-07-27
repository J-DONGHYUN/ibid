package project.kjhjdh.ibid.inspection.application;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.order.application.OrderService;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final OrderService orderService;

    public void receive(Long orderId) {
        orderService.startInspection(orderId);
    }
}
