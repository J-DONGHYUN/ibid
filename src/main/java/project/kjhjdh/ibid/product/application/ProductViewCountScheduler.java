package project.kjhjdh.ibid.product.application;

import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class ProductViewCountScheduler {

    private final ProductViewCountFlusher productViewCountFlusher;

    @Scheduled(fixedDelayString = "${product.view.flush-interval}")
    public void flushViewCounts() {
        productViewCountFlusher.flush();
    }

    @EventListener(ContextClosedEvent.class)
    public void flushOnShutdown() {
        productViewCountFlusher.flush();
    }
}
