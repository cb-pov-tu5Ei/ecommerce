package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.*;
import io.cb_demos.ecommerce.exception.InsufficientStockException;
import io.cb_demos.ecommerce.exception.InvalidOrderStateException;
import io.cb_demos.ecommerce.exception.OrderNotFoundException;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("50.00"));
        testProduct.setStockQuantity(10);

        testCartItem = new CartItem();
        testCartItem.setProductId(1L);
        testCartItem.setProductName("Test Product");
        testCartItem.setPrice(new BigDecimal("50.00"));
        testCartItem.setQuantity(2);
    }

    @Test
    void createOrder_shouldCreateOrderSuccessfully() {
        TestDelayUtil.mediumDelay();
        // Given
        List<CartItem> cartItems = Arrays.asList(testCartItem);
        when(cartService.getCartItems()).thenReturn(cartItems);
        when(productService.isInStock(1L, 2)).thenReturn(true);
        when(productService.findById(1L)).thenReturn(testProduct);
        when(shippingService.getDefaultShippingMethod()).thenReturn(ShippingMethod.STANDARD);
        when(shippingService.calculateShippingCost(any(BigDecimal.class), any(ShippingMethod.class)))
                .thenReturn(new BigDecimal("5.99"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        doNothing().when(productService).updateStock(1L, -2);
        doNothing().when(cartService).clearCart();

        // When
        Order result = orderService.createOrder(testUser, "123 Main St");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getShippingAddress()).isEqualTo("123 Main St");
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("105.99"));
        assertThat(result.getShippingMethod()).isEqualTo(ShippingMethod.STANDARD);
        assertThat(result.getShippingCost()).isEqualByComparingTo(new BigDecimal("5.99"));
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(result.getOrderItems()).hasSize(1);

        verify(orderRepository).save(any(Order.class));
        verify(productService).updateStock(1L, -2);
        verify(cartService).clearCart();
    }

    @Test
    void createOrder_shouldThrowException_whenCartIsEmpty() {
        // Given
        when(cartService.getCartItems()).thenReturn(new ArrayList<>());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(testUser, "123 Main St"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty cart");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_shouldThrowException_whenInsufficientStock() {
        TestDelayUtil.mediumDelay();
        // Given
        List<CartItem> cartItems = Arrays.asList(testCartItem);
        when(cartService.getCartItems()).thenReturn(cartItems);
        when(productService.isInStock(1L, 2)).thenReturn(false);
        when(productService.findById(1L)).thenReturn(testProduct);

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(testUser, "123 Main St"))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any());
        verify(productService, never()).updateStock(anyLong(), anyInt());
    }

    @Test
    void findById_shouldReturnOrder_whenOrderExists() {
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-123");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When
        Order result = orderService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(orderRepository).findById(1L);
    }

    @Test
    void findById_shouldThrowException_whenOrderNotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.findById(999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void updateOrderStatus_shouldUpdateStatus() {
        TestDelayUtil.mediumDelay();
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setStatus(Order.OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        Order result = orderService.updateOrderStatus(1L, Order.OrderStatus.SHIPPED);

        // Then
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.SHIPPED);
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_shouldRestoreStockAndUpdateStatus() {
        TestDelayUtil.mediumDelay();
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setStatus(Order.OrderStatus.PENDING);

        OrderItem orderItem1 = new OrderItem();
        orderItem1.setProduct(testProduct);
        orderItem1.setQuantity(2);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setStockQuantity(5);

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setProduct(product2);
        orderItem2.setQuantity(3);

        order.setOrderItems(Arrays.asList(orderItem1, orderItem2));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        doNothing().when(productService).updateStock(anyLong(), anyInt());

        // When
        Order result = orderService.cancelOrder(1L, testUser);

        // Then
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        verify(productService).updateStock(1L, 2);
        verify(productService).updateStock(2L, 3);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_shouldThrowException_whenUserNotOwner() {
        TestDelayUtil.mediumDelay();
        // Given
        User differentUser = new User();
        differentUser.setId(2L);
        differentUser.setUsername("otheruser");

        Order order = new Order();
        order.setId(1L);
        order.setUser(differentUser);
        order.setStatus(Order.OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, testUser))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("not own");

        verify(productService, never()).updateStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_shouldThrowException_whenOrderAlreadyCancelled() {
        TestDelayUtil.mediumDelay();
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setStatus(Order.OrderStatus.CANCELLED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, testUser))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("already cancelled");

        verify(productService, never()).updateStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_shouldThrowException_whenOrderShipped() {
        TestDelayUtil.mediumDelay();
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setStatus(Order.OrderStatus.SHIPPED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, testUser))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("cannot be cancelled");

        verify(productService, never()).updateStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_shouldRecordCancellationTimestamp() {
        TestDelayUtil.mediumDelay();
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setOrderItems(new ArrayList<>());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        LocalDateTime beforeCancel = LocalDateTime.now();

        // When
        Order result = orderService.cancelOrder(1L, testUser);

        // Then
        assertThat(result.getCancelledAt()).isNotNull();
        assertThat(result.getCancelledAt()).isAfterOrEqualTo(beforeCancel);
        assertThat(result.getCancelledAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void cancelOrder_shouldRecordCancellationReason() {
        TestDelayUtil.mediumDelay();
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setUser(testUser);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        order.setOrderItems(new ArrayList<>());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        String reason = "Changed my mind";

        // When
        Order result = orderService.cancelOrder(1L, testUser, reason);

        // Then
        assertThat(result.getCancellationReason()).isEqualTo(reason);
        verify(orderRepository).save(order);
    }
}
