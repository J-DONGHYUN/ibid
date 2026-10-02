package project.kjhjdh.ibid.trade.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import project.kjhjdh.ibid.chat.application.ChatRoomService;
import project.kjhjdh.ibid.common.event.NotificationMessage;
import project.kjhjdh.ibid.common.event.NotificationMessage.NotificationType;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.DeviceSpecFixture;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.domain.ProductCondition;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long SELLER_ID = 10L;
    private static final Long BUYER_ID = 20L;
    private static final Long OTHER_USER_ID = 30L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReservationService reservationService;

    @DisplayName("[TR-01] 판매자가 채팅 상대를 예약하면 상품이 예약중이 되고 예약 상대가 지정된다")
    @Test
    void reserve() {
        // given
        Product product = onSaleProduct();
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
        given(chatRoomService.isChatPartner(PRODUCT_ID, BUYER_ID)).willReturn(true);

        // when
        reservationService.reserve(PRODUCT_ID, SELLER_ID, BUYER_ID);

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.RESERVED);
        assertThat(product.getReservedBuyerId()).isEqualTo(BUYER_ID);
        then(eventPublisher).should().publishEvent(
                new NotificationMessage(BUYER_ID, NotificationType.RESERVED, PRODUCT_ID));
    }

    @DisplayName("[NT-02] 예약을 해제하면 예약 상대에게 해제 알림 이벤트를 낸다")
    @Test
    void cancelReservation_notifiesBuyer() {
        // given
        Product product = onSaleProduct();
        product.reserve(BUYER_ID);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        reservationService.cancelReservation(PRODUCT_ID, SELLER_ID);

        // then
        then(eventPublisher).should().publishEvent(
                new NotificationMessage(BUYER_ID, NotificationType.RESERVATION_CANCELED, PRODUCT_ID));
    }

    @DisplayName("[TR-01] 판매자 본인이 아니면 예약할 수 없다")
    @Test
    void reserve_notOwner() {
        // given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(onSaleProduct()));

        // when & then
        assertThatThrownBy(() -> reservationService.reserve(PRODUCT_ID, OTHER_USER_ID, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @DisplayName("[I-04] 그 상품의 채팅 상대가 아니면 예약할 수 없다")
    @Test
    void reserve_notChatPartner() {
        // given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(onSaleProduct()));
        given(chatRoomService.isChatPartner(PRODUCT_ID, BUYER_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> reservationService.reserve(PRODUCT_ID, SELLER_ID, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.NOT_CHAT_PARTNER.getMessage());
    }

    @DisplayName("[TR-01] 없는 상품은 예약할 수 없다")
    @Test
    void reserve_productNotFound() {
        // given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.reserve(PRODUCT_ID, SELLER_ID, BUYER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    @DisplayName("[TR-02] 판매자가 예약을 해제하면 상품이 판매중으로 돌아간다")
    @Test
    void cancelReservation() {
        // given
        Product product = onSaleProduct();
        product.reserve(BUYER_ID);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when
        reservationService.cancelReservation(PRODUCT_ID, SELLER_ID);

        // then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getReservedBuyerId()).isNull();
    }

    @DisplayName("[TR-02] 판매자 본인이 아니면 예약을 해제할 수 없다")
    @Test
    void cancelReservation_notOwner() {
        // given
        Product product = onSaleProduct();
        product.reserve(BUYER_ID);
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> reservationService.cancelReservation(PRODUCT_ID, OTHER_USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ACCESS_DENIED.getMessage());
    }

    @DisplayName("[TR-02] 없는 상품은 예약을 해제할 수 없다")
    @Test
    void cancelReservation_productNotFound() {
        // given
        given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.cancelReservation(PRODUCT_ID, SELLER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
    }

    private Product onSaleProduct() {
        Product product = Product.create(SELLER_ID, "아이폰 13", "A급", 500000,
                ProductCondition.USED, DeviceSpecFixture.sample());
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        return product;
    }
}
