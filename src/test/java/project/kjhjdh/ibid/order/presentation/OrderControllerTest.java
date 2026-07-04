package project.kjhjdh.ibid.order.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import project.kjhjdh.ibid.auth.domain.UserInfo;
import project.kjhjdh.ibid.auth.presentation.interceptor.AuthenticationInterceptor;
import project.kjhjdh.ibid.auth.presentation.resolver.LoginUser;
import project.kjhjdh.ibid.common.config.WebConfig;
import project.kjhjdh.ibid.common.exception.BusinessException;
import project.kjhjdh.ibid.common.exception.ErrorCode;
import project.kjhjdh.ibid.order.application.OrderService;
import project.kjhjdh.ibid.order.presentation.dto.PurchaseRequest;
import project.kjhjdh.ibid.support.ControllerTestSupport;

@ActiveProfiles("test")
@Import(OrderControllerTest.LoginUserArgumentResolverTestConfig.class)
@WebMvcTest(controllers = OrderController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {WebConfig.class, AuthenticationInterceptor.class}))
class OrderControllerTest extends ControllerTestSupport {

    private static final Long LOGIN_USER_ID = 1L;

    @MockitoBean
    private OrderService orderService;

    @DisplayName("즉시구매에 성공하면 201과 orderId를 응답한다")
    @Test
    void purchase() {
        // given
        given(orderService.purchase(anyLong(), any())).willReturn(100L);

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new PurchaseRequest(1L, 2))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("orderId", equalTo(100));
    }

    @DisplayName("구매 수량이 0 이하이면 400을 응답한다")
    @Test
    void purchase_invalidQuantity() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new PurchaseRequest(1L, 0))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("본인이 등록한 상품을 구매하면 400을 응답한다")
    @Test
    void purchase_selfTrade() {
        // given
        willThrow(new BusinessException(ErrorCode.SELF_TRADE_NOT_ALLOWED))
                .given(orderService).purchase(anyLong(), any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new PurchaseRequest(1L, 1))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("SELF_TRADE_NOT_ALLOWED"));
    }

    @DisplayName("재고가 부족하면 409를 응답한다")
    @Test
    void purchase_insufficientStock() {
        // given
        willThrow(new BusinessException(ErrorCode.INSUFFICIENT_STOCK))
                .given(orderService).purchase(anyLong(), any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new PurchaseRequest(1L, 99))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo("INSUFFICIENT_STOCK"));
    }

    @DisplayName("존재하지 않는 상품을 구매하면 404를 응답한다")
    @Test
    void purchase_productNotFound() {
        // given
        willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                .given(orderService).purchase(anyLong(), any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new PurchaseRequest(999L, 1))
                .when()
                .post("/api/orders")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("PRODUCT_NOT_FOUND"));
    }

    @TestConfiguration
    static class LoginUserArgumentResolverTestConfig implements WebMvcConfigurer {

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {

                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.hasParameterAnnotation(LoginUser.class);
                }

                @Override
                public Object resolveArgument(MethodParameter parameter,
                                              ModelAndViewContainer mavContainer,
                                              NativeWebRequest webRequest,
                                              WebDataBinderFactory binderFactory) {
                    return new UserInfo(LOGIN_USER_ID);
                }
            });
        }
    }
}
