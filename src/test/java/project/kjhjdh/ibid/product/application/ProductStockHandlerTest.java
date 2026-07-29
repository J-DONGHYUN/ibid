package project.kjhjdh.ibid.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.product.domain.Product;
import project.kjhjdh.ibid.product.infra.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductStockHandlerTest {

	private static final Long PRODUCT_ID = 1L;
	private static final Long SELLER_ID = 10L;

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private ProductStockHandler productStockHandler;

	@DisplayName("재고를 감소시키면 남은 재고가 요청 수량만큼 줄어든다")
	@Test
	void decreaseStock() {
		// given
		Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 5);
		product.openForSale();
		given(productRepository.findByIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(product));

		// when
		productStockHandler.decreaseStock(PRODUCT_ID, 2);

		// then
		assertThat(product.getStock()).isEqualTo(3);
	}

	@DisplayName("존재하지 않는 상품의 재고를 감소시키려 하면 PRODUCT_NOT_FOUND 예외를 던진다")
	@Test
	void decreaseStock_productNotFound() {
		// given
		given(productRepository.findByIdForUpdate(PRODUCT_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> productStockHandler.decreaseStock(PRODUCT_ID, 2))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
	}

	@DisplayName("남은 재고보다 많은 수량을 감소시키려 하면 INSUFFICIENT_STOCK 예외를 던진다")
	@Test
	void decreaseStock_insufficientStock() {
		// given
		Product product = Product.create(SELLER_ID, "나이키 후드", "상태 좋음", 89000, 1);
		product.openForSale();
		given(productRepository.findByIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(product));

		// when & then
		assertThatThrownBy(() -> productStockHandler.decreaseStock(PRODUCT_ID, 2))
				.isInstanceOf(BusinessException.class)
				.hasMessage(ErrorCode.INSUFFICIENT_STOCK.getMessage());
	}
}
