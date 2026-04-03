package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.*;
import io.cb_demos.ecommerce.exception.MinimumOrderValueException;
import io.cb_demos.ecommerce.repository.OrderRepository;
import io.cb_demos.ecommerce.service.impl.OrderServiceImpl;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinimumOrderValueTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartService cartService;

    @Mock
    private ProductService productService;

    @Mock
    private PromoCodeService promoCodeService;

    @Mock
    private ShippingService shippingService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private Product testProduct;
    private ShippingMethod shippingMethod;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("10.00"));
        testProduct.setStockQuantity(100);

        shippingMethod = ShippingMethod.STANDARD;
    }

    @Test
    void shouldThrowExceptionWhenOrderTotalBelowMinimum() {
        TestDelayUtil.mediumDelay();

        // Cart with only $10 worth of items (below $20 minimum)
        CartItem cartItem = new CartItem(1L, "Test Product", new BigDecimal("10.00"), 1, null);
        when(cartService.getCartItems()).thenReturn(List.of(cartItem));
        // No need to mock stock checks - validation happens before stock verification

        assertThatThrownBy(() -> orderService.createOrder(testUser, "123 Test St"))
                .isInstanceOf(MinimumOrderValueException.class)
                .hasMessageContaining("does not meet minimum order value")
                .hasMessageContaining("$10.00")
                .hasMessageContaining("$20.00");

        verify(orderRepository, never()).save(any(Order.class));
        verify(cartService, never()).clearCart();
    }

    @Test
    void shouldThrowExceptionWhenOrderTotalExactlyOneDollarBelowMinimum() {
        TestDelayUtil.mediumDelay();

        // Cart with $19.00 worth of items (just below $20 minimum)
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("19.00"));
        product.setStockQuantity(100);

        CartItem cartItem = new CartItem(1L, "Test Product", new BigDecimal("19.00"), 1, null);
        when(cartService.getCartItems()).thenReturn(List.of(cartItem));
        // No need to mock stock checks - validation happens before stock verification

        assertThatThrownBy(() -> orderService.createOrder(testUser, "123 Test St"))
                .isInstanceOf(MinimumOrderValueException.class);

        MinimumOrderValueException exception = catchThrowableOfType(
                () -> orderService.createOrder(testUser, "123 Test St"),
                MinimumOrderValueException.class
        );

        assertThat(exception.getCurrentTotal()).isEqualByComparingTo(new BigDecimal("19.00"));
        assertThat(exception.getMinimumRequired()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(exception.getAmountNeeded()).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    void shouldSucceedWhenOrderTotalExactlyMeetsMinimum() {
        TestDelayUtil.mediumDelay();

        // Cart with exactly $20.00 worth of items
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("20.00"));
        product.setStockQuantity(100);

        CartItem cartItem = new CartItem(1L, "Test Product", new BigDecimal("20.00"), 1, null);
        when(cartService.getCartItems()).thenReturn(List.of(cartItem));
        when(productService.isInStock(anyLong(), anyInt())).thenReturn(true);
        when(productService.findById(anyLong())).thenReturn(product);
        when(shippingService.getDefaultShippingMethod()).thenReturn(shippingMethod);
        when(shippingService.calculateShippingCost(any(BigDecimal.class), any(ShippingMethod.class)))
                .thenReturn(new BigDecimal("5.00"));

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setOrderNumber("ORD-123");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        assertThatCode(() -> orderService.createOrder(testUser, "123 Test St"))
                .doesNotThrowAnyException();

        verify(orderRepository).save(any(Order.class));
        verify(cartService).clearCart();
    }

    @Test
    void shouldSucceedWhenOrderTotalExceedsMinimum() {
        TestDelayUtil.mediumDelay();

        // Cart with $50.00 worth of items (well above $20 minimum)
        Product product = new Product();
        product.setId(1L);
        product.setName("Expensive Product");
        product.setPrice(new BigDecimal("50.00"));
        product.setStockQuantity(100);

        CartItem cartItem = new CartItem(1L, "Expensive Product", new BigDecimal("50.00"), 1, null);
        when(cartService.getCartItems()).thenReturn(List.of(cartItem));
        when(productService.isInStock(anyLong(), anyInt())).thenReturn(true);
        when(productService.findById(anyLong())).thenReturn(product);
        when(shippingService.getDefaultShippingMethod()).thenReturn(shippingMethod);
        when(shippingService.calculateShippingCost(any(BigDecimal.class), any(ShippingMethod.class)))
                .thenReturn(new BigDecimal("5.00"));

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setOrderNumber("ORD-456");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        assertThatCode(() -> orderService.createOrder(testUser, "123 Test St"))
                .doesNotThrowAnyException();

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldCalculateMinimumBasedOnSubtotalBeforeShipping() {
        TestDelayUtil.mediumDelay();

        // Cart with $20.00 subtotal should pass even though total with shipping > $20
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("20.00"));
        product.setStockQuantity(100);

        CartItem cartItem = new CartItem(1L, "Test Product", new BigDecimal("20.00"), 1, null);
        when(cartService.getCartItems()).thenReturn(List.of(cartItem));
        when(productService.isInStock(anyLong(), anyInt())).thenReturn(true);
        when(productService.findById(anyLong())).thenReturn(product);
        when(shippingService.getDefaultShippingMethod()).thenReturn(shippingMethod);
        when(shippingService.calculateShippingCost(any(BigDecimal.class), any(ShippingMethod.class)))
                .thenReturn(new BigDecimal("10.00")); // High shipping cost

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setOrderNumber("ORD-789");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        assertThatCode(() -> orderService.createOrder(testUser, "123 Test St"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowExceptionWithMultipleItemsBelowMinimum() {
        TestDelayUtil.mediumDelay();

        // Multiple items totaling $15.00 (below $20 minimum)
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setPrice(new BigDecimal("7.00"));
        product1.setStockQuantity(100);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setPrice(new BigDecimal("8.00"));
        product2.setStockQuantity(100);

        CartItem item1 = new CartItem(1L, "Product 1", new BigDecimal("7.00"), 1, null);
        CartItem item2 = new CartItem(2L, "Product 2", new BigDecimal("8.00"), 1, null);

        when(cartService.getCartItems()).thenReturn(List.of(item1, item2));
        // No need to mock stock checks - validation happens before stock verification

        assertThatThrownBy(() -> orderService.createOrder(testUser, "123 Test St"))
                .isInstanceOf(MinimumOrderValueException.class)
                .hasMessageContaining("$15.00")
                .hasMessageContaining("$20.00");
    }

    @Test
    void shouldThrowExceptionBeforeCheckingStock() {
        TestDelayUtil.mediumDelay();

        // Verify minimum order validation happens before stock check
        CartItem cartItem = new CartItem(1L, "Test Product", new BigDecimal("5.00"), 1, null);
        when(cartService.getCartItems()).thenReturn(List.of(cartItem));

        assertThatThrownBy(() -> orderService.createOrder(testUser, "123 Test St"))
                .isInstanceOf(MinimumOrderValueException.class);

        // Stock check should never be called if minimum validation fails
        verify(productService, never()).isInStock(anyLong(), anyInt());
    }

    @Test
    void shouldThrowExceptionBeforeReducingStock() {
        TestDelayUtil.mediumDelay();

        // Verify stock is not reduced if minimum order validation fails
        CartItem cartItem = new CartItem(1L, "Test Product", new BigDecimal("10.00"), 1, null);
        when(cartService.getCartItems()).thenReturn(List.of(cartItem));
        // No need to mock stock checks - validation happens before stock verification

        assertThatThrownBy(() -> orderService.createOrder(testUser, "123 Test St"))
                .isInstanceOf(MinimumOrderValueException.class);

        // Stock should never be updated if validation fails
        verify(productService, never()).updateStock(anyLong(), anyInt());
    }

    @Test
    void shouldValidateMinimumForOrderWithPromoCode() {
        TestDelayUtil.mediumDelay();

        // Order with $25 subtotal and 30% discount = $17.50 final (below $20 minimum)
        // Minimum should be checked BEFORE discount is applied
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(new BigDecimal("25.00"));
        product.setStockQuantity(100);

        CartItem cartItem = new CartItem(1L, "Test Product", new BigDecimal("25.00"), 1, null);
        when(cartService.getCartItems()).thenReturn(List.of(cartItem));
        when(productService.isInStock(anyLong(), anyInt())).thenReturn(true);
        when(productService.findById(anyLong())).thenReturn(product);
        when(shippingService.getDefaultShippingMethod()).thenReturn(shippingMethod);
        when(shippingService.calculateShippingCost(any(BigDecimal.class), any(ShippingMethod.class)))
                .thenReturn(new BigDecimal("5.00"));
        when(promoCodeService.calculateDiscount(any(BigDecimal.class), anyString()))
                .thenReturn(new BigDecimal("7.50")); // 30% discount

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setOrderNumber("ORD-PROMO");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Should succeed because validation happens before discount
        assertThatCode(() -> orderService.createOrder(testUser, "123 Test St", "SAVE30"))
                .doesNotThrowAnyException();
    }
}
