package project.kjhjdh.ibid.inspection.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import project.kjhjdh.ibid.inspection.domain.Inspection;
import project.kjhjdh.ibid.inspection.domain.InspectionFailed;
import project.kjhjdh.ibid.inspection.domain.InspectionPassed;
import project.kjhjdh.ibid.inspection.infra.InspectionRepository;
import project.kjhjdh.ibid.inspection.presentation.dto.InspectionQueueResponse;
import project.kjhjdh.ibid.order.application.OrderService;
import project.kjhjdh.ibid.order.domain.Order;
import project.kjhjdh.ibid.order.domain.OrderStatus;
import project.kjhjdh.ibid.order.infra.OrderRepository;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private static final List<OrderStatus> QUEUE_STATUSES =
            List.of(OrderStatus.SHIPPED_TO_INSPECTOR, OrderStatus.UNDER_INSPECTION);

    private final OrderService orderService;
    private final InspectionRepository inspectionRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public InspectionQueueResponse getQueue() {
        List<Order> orders = orderRepository.findByStatusInOrderByIdDesc(QUEUE_STATUSES);

        Map<Long, Product> productsById = productRepository.findAllById(
                orders.stream().map(Order::getProductId).distinct().toList()
        ).stream().collect(Collectors.toMap(Product::getId, product -> product));

        List<InspectionQueueResponse.Item> items = orders.stream()
                .map(order -> InspectionQueueResponse.Item.of(order, productsById.get(order.getProductId())))
                .toList();
        return new InspectionQueueResponse(items);
    }

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
