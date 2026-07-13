package project.kjhjdh.ibid.product.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import project.kjhjdh.ibid.product.application.ProductService;
import project.kjhjdh.ibid.product.domain.ProductStatus;
import project.kjhjdh.ibid.product.presentation.dto.ProductDetailResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductListResponse;
import project.kjhjdh.ibid.product.presentation.dto.ProductRegisterRequest;
import project.kjhjdh.ibid.product.presentation.dto.ProductSummaryResponse;
import project.kjhjdh.ibid.support.ControllerTestSupport;

@ActiveProfiles("test")
@Import(ProductControllerTest.LoginUserArgumentResolverTestConfig.class)
@WebMvcTest(controllers = ProductController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {WebConfig.class, AuthenticationInterceptor.class}))
class ProductControllerTest extends ControllerTestSupport {

    private static final Long LOGIN_USER_ID = 1L;

    @MockitoBean
    private ProductService productService;

    @DisplayName("판매 등록에 성공하면 201과 productId를 응답한다")
    @Test
    void register() {
        // given
        given(productService.register(anyLong(), any())).willReturn(10L);

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ProductRegisterRequest("나이키 후드", "상태 좋음", 89000, 3))
                .when()
                .post("/api/products")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("productId", equalTo(10));
    }

    @DisplayName("제목이 비어 있으면 400을 응답한다")
    @Test
    void register_blankTitle() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ProductRegisterRequest("", "상태 좋음", 89000, 3))
                .when()
                .post("/api/products")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("판매가가 0 이하이면 400을 응답한다")
    @Test
    void register_invalidPrice() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ProductRegisterRequest("나이키 후드", "상태 좋음", 0, 3))
                .when()
                .post("/api/products")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("재고 수량이 0 이하이면 400을 응답한다")
    @Test
    void register_invalidStock() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new ProductRegisterRequest("나이키 후드", "상태 좋음", 89000, 0))
                .when()
                .post("/api/products")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("INVALID_INPUT"));
    }

    @DisplayName("상품 목록 조회에 성공하면 200과 상품 목록을 응답한다")
    @Test
    void getProducts() {
        // given
        given(productService.getProducts(any())).willReturn(new ProductListResponse(
                List.of(
                        new ProductSummaryResponse(2L, "나이키 후드", 89000, 3, ProductStatus.ON_SALE),
                        new ProductSummaryResponse(1L, "아디다스 슬리퍼", 30000, 0, ProductStatus.SOLD_OUT)
                ),
                1L, true
        ));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/products")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("products.size()", equalTo(2))
                .body("products[0].productId", equalTo(2))
                .body("products[0].stock", equalTo(3))
                .body("products[0].status", equalTo("ON_SALE"))
                .body("products[1].status", equalTo("SOLD_OUT"))
                .body("nextCursor", equalTo(1))
                .body("hasNext", equalTo(true));
    }

    @DisplayName("상품 상세 조회에 성공하면 200과 상품 정보를 응답한다")
    @Test
    void getProduct() {
        // given
        given(productService.getProduct(1L)).willReturn(
                new ProductDetailResponse(1L, 5L, "나이키 후드", "상태 좋음", 89000, 3, ProductStatus.ON_SALE));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/products/{productId}", 1L)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("productId", equalTo(1))
                .body("sellerId", equalTo(5))
                .body("title", equalTo("나이키 후드"))
                .body("description", equalTo("상태 좋음"))
                .body("stock", equalTo(3))
                .body("status", equalTo("ON_SALE"));
    }

    @DisplayName("존재하지 않는 상품을 조회하면 404를 응답한다")
    @Test
    void getProduct_notFound() {
        // given
        willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                .given(productService).getProduct(eq(999L));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/products/{productId}", 999L)
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
