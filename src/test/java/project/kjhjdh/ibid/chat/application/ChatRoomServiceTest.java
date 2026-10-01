package project.kjhjdh.ibid.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import project.kjhjdh.ibid.chat.domain.ChatRoom;
import project.kjhjdh.ibid.chat.infra.ChatRoomRepository;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @DisplayName("[CH-01] 구매 희망자가 상품에 채팅방을 열면 상품·판매자·구매자로 방이 생긴다")
    @Test
    void open() {
        // given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product()));
        given(chatRoomRepository.findByProductIdAndBuyerId(PRODUCT_ID, BUYER_ID)).willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatRoom room = chatRoomService.open(PRODUCT_ID, BUYER_ID);

        // then
        assertThat(room.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(room.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(room.getBuyerId()).isEqualTo(BUYER_ID);
    }

    @DisplayName("[CH-01] 이미 열린 방이 있으면 새로 만들지 않고 기존 방을 돌려준다")
    @Test
    void open_idempotent() {
        // given
        ChatRoom existing = ChatRoom.open(PRODUCT_ID, SELLER_ID, BUYER_ID);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product()));
        given(chatRoomRepository.findByProductIdAndBuyerId(PRODUCT_ID, BUYER_ID)).willReturn(Optional.of(existing));

        // when
        ChatRoom room = chatRoomService.open(PRODUCT_ID, BUYER_ID);

        // then
        assertThat(room).isSameAs(existing);
        then(chatRoomRepository).should(never()).save(any());
    }

    @DisplayName("[I-05] 판매자 본인은 자기 상품에 채팅방을 열 수 없다")
    @Test
    void open_ownProduct() {
        // given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product()));
        given(chatRoomRepository.findByProductIdAndBuyerId(PRODUCT_ID, SELLER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomService.open(PRODUCT_ID, SELLER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CANNOT_OPEN_CHAT_ON_OWN_PRODUCT.getMessage());
    }

    @DisplayName("[CH-01] 존재하지 않는 상품에는 채팅방을 열 수 없다")
    @Test
    void open_productNotFound() {
        // given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatRoomService.open(PRODUCT_ID, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    private Product product() {
        Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000,
                ProductCondition.USED, DeviceSpecFixture.sample());
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        return product;
    }
}
