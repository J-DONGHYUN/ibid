package project.kjhjdh.ibid.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

import project.kjhjdh.ibid.chat.application.ChatMessageService;
import project.kjhjdh.ibid.chat.application.SendMessageCommand;
import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.notification.domain.Notification;
import project.kjhjdh.ibid.notification.infra.NotificationRepository;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class NotificationBrokerRestartTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final int AMQP_PORT = 5672;
    private static final int FIXED_HOST_PORT = freePort();

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedPortRabbitConfiguration {

        @Bean
        @ServiceConnection
        RabbitMQContainer rabbitContainer() {
            return new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"))
                    .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                            .withPublishAllPorts(true)
                            .withPortBindings(new PortBinding(Ports.Binding.bindPort(FIXED_HOST_PORT), new ExposedPort(AMQP_PORT))));
        }
    }

    @Autowired
    private RabbitMQContainer rabbitContainer;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @DisplayName("[I-11] 브로커가 재시작된 뒤 새 알림은 다시 소비되고, 장애 중에 발행한 알림은 돌아오지 않는다 (QA-A.4 · A.3)")
    @Test
    void broker_restart_resumesConsumptionAndLosesOutageNotifications() {
        // given — 호스트 포트가 고정된 브로커. 재시작해도 포트가 바뀌지 않아야 한다
        assertThat(rabbitContainer.getMappedPort(AMQP_PORT)).as("전제: 고정한 호스트 포트가 적용됐다").isEqualTo(FIXED_HOST_PORT);
        Long before = sendFromBuyer();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(referenceIds()).as("장애 전 알림 소비 (전제)").contains(before));

        // when — 브로커를 멈추고, 장애 중에 메시지를 보내고, 브로커를 다시 올린다
        String containerId = rabbitContainer.getContainerId();
        rabbitContainer.getDockerClient().stopContainerCmd(containerId).exec();
        Long during = sendFromBuyer();
        rabbitContainer.getDockerClient().startContainerCmd(containerId).exec();
        assertThat(rabbitContainer.getMappedPort(AMQP_PORT)).as("재시작 뒤 호스트 포트").isEqualTo(FIXED_HOST_PORT);
        await().atMost(Duration.ofSeconds(60)).ignoreExceptions().untilAsserted(() ->
                assertThat(connectionFactory.createConnection().isOpen()).as("재시작 뒤 연결 가능 여부").isTrue());
        Long after = sendFromBuyer();

        // then — 복구 뒤 새 알림은 소비되고, 장애 중 알림은 유실된 채 돌아오지 않는다
        await().atMost(Duration.ofSeconds(60)).untilAsserted(() ->
                assertThat(referenceIds()).as("복구 뒤 알림 소비 (장애 전 %d · 장애 중 %d · 복구 뒤 %d)", before, during, after).contains(after));
        assertThat(referenceIds()).as("장애 중에 발행한 알림은 돌아오지 않는다 (ADR-0009 가 받아들인 유실)").doesNotContain(during);
    }

    private List<Long> referenceIds() {
        return notificationRepository.findAll().stream().map(Notification::getReferenceId).toList();
    }

    private Long sendFromBuyer() {
        Long productId = productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
        Long roomId = chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID)).getId();
        chatMessageService.send(new SendMessageCommand(roomId, BUYER_ID, "c-" + roomId, "안녕하세요"));
        return roomId;
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
