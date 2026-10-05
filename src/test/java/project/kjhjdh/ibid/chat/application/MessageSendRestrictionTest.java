package project.kjhjdh.ibid.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatMessageRepository;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.application.ProductService;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;
import project.kjhjdh.ibid.support.IntegrationTestSupport;
import project.kjhjdh.ibid.trade.application.CompletionService;

class MessageSendRestrictionTest extends IntegrationTestSupport {

    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private CompletionService completionService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @DisplayName("[I-09] 삭제된 상품의 채팅방에는 메시지를 보낼 수 없다 (QA-1.6 대조군 — 서비스 삭제 검사)")
    @Test
    void send_deletedProduct() {
        // given — 채팅방이 있는 상품을 판매자가 삭제한다
        Long productId = saveProduct();
        Long roomId = openRoom(productId);
        productService.delete(SELLER_ID, productId);

        // when
        assertThatThrownBy(() -> chatMessageService.send(
                new SendMessageCommand(roomId, BUYER_ID, "c-1", "아직 거래 가능한가요?")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_DELETED.getMessage());

        // then — 메시지가 저장되지 않는다
        assertThat(chatMessageRepository.findByChatRoomIdAndClientMessageId(roomId, "c-1")).isEmpty();
    }

    @DisplayName("[I-09] 거래완료된 상품의 채팅방에는 메시지를 보낼 수 있다 (QA-3.4 — 인수 조율)")
    @Test
    void send_soldProduct() {
        // given — 거래완료된 상품의 거래 상대 채팅방
        Long productId = saveProduct();
        Long roomId = openRoom(productId);
        completionService.complete(productId, SELLER_ID, BUYER_ID);

        // when — 거래 뒤 인수 시각을 조율한다
        SendMessageResult result = chatMessageService.send(
                new SendMessageCommand(roomId, BUYER_ID, "c-1", "몇 시에 뵐까요?"));

        // then — I-09 는 삭제만 막는다. 거래완료는 대화를 끊지 않는다
        assertThat(result.created()).isTrue();
        assertThat(chatMessageRepository.findByChatRoomIdAndClientMessageId(roomId, "c-1")).isPresent();
    }

    private Long openRoom(Long productId) {
        return chatRoomRepository.save(ChatRoom.open(productId, SELLER_ID, BUYER_ID)).getId();
    }

    private Long saveProduct() {
        return productRepository.save(Product.create(
                SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample())).getId();
    }
}
