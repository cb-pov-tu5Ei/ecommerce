package io.cb_demos.ecommerce.controller;
import io.cb_demos.ecommerce.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;

import io.cb_demos.ecommerce.domain.Order;
import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.exception.InvalidOrderStateException;
import io.cb_demos.ecommerce.service.OrderService;
import io.cb_demos.ecommerce.service.CartService;
import io.cb_demos.ecommerce.service.UserService;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestSecurityConfig.class)
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private OrderService orderService;

    private Order testOrder;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-2024-001");
        testOrder.setUser(testUser);
        testOrder.setTotalAmount(new BigDecimal("149.99"));
        testOrder.setStatus(Order.OrderStatus.PENDING);
        testOrder.setShippingAddress("123 Test Street");
        testOrder.setOrderDate(LocalDateTime.now());

        // Set up default mocks
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(cartService.getTotalItems()).thenReturn(1);
    }

    @Test
    @WithMockUser(username = "testuser")
    void checkout_shouldDisplayCheckoutForm() throws Exception {
        // When & Then
        mockMvc.perform(get("/orders/checkout").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/checkout"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void placeOrder_shouldCreateOrderAndRedirect() throws Exception {
        TestDelayUtil.mediumDelay();
        // Given
        when(orderService.createOrder(any(User.class), eq("123 Test Street")))
                .thenReturn(testOrder);

        // When & Then
        mockMvc.perform(post("/orders/place")
                        .with(csrf()).with(user("testuser"))
                        .param("shippingAddress", "123 Test Street"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/confirmation/1"));

        verify(orderService).createOrder(any(User.class), eq("123 Test Street"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void placeOrder_shouldRejectEmptyAddress() throws Exception {
        // When & Then
        mockMvc.perform(post("/orders/place")
                        .with(csrf()).with(user("testuser"))
                        .param("shippingAddress", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/checkout"));

        verify(orderService, never()).createOrder(any(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void orderConfirmation_shouldDisplayOrderDetails() throws Exception {
        // Given
        when(orderService.findById(1L)).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(get("/orders/confirmation/1").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/confirmation"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("order", testOrder));

        verify(orderService).findById(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void orderDetail_shouldDisplayOrderDetails() throws Exception {
        // Given
        when(orderService.findById(1L)).thenReturn(testOrder);

        // When & Then
        mockMvc.perform(get("/orders/1").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/detail"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("order", testOrder));

        verify(orderService).findById(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void listOrders_shouldDisplayUserOrders() throws Exception {
        TestDelayUtil.mediumDelay();
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 1);
        when(orderService.findUserOrders(any(User.class), any())).thenReturn(orderPage);

        // When & Then
        mockMvc.perform(get("/orders").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attributeExists("currentPage"));

        verify(orderService).findUserOrders(any(User.class), any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void listOrders_shouldHandlePagination() throws Exception {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(2, 10), 25);
        when(orderService.findUserOrders(any(User.class), any())).thenReturn(orderPage);

        // When & Then
        mockMvc.perform(get("/orders")
                        .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"))
                .andExpect(model().attribute("currentPage", 2))
                .andExpect(model().attribute("totalPages", 3));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"ADMIN"})
    void cancelOrder_shouldUpdateOrderStatus() throws Exception {
        // Given
        testOrder.setStatus(Order.OrderStatus.CANCELLED);
        when(orderService.cancelOrder(eq(1L), any(User.class), isNull()))
                .thenReturn(testOrder);

        // When & Then
        mockMvc.perform(post("/orders/1/cancel")
                        .with(csrf()).with(user("testuser")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"));

        verify(orderService).cancelOrder(eq(1L), any(User.class), isNull());
    }

    @Test
    @WithMockUser(username = "testuser")
    void orderDetail_shouldHandleNonExistentOrder() throws Exception {
        // Given
        when(orderService.findById(999L))
                .thenThrow(new IllegalArgumentException("Order not found"));

        // When & Then
        mockMvc.perform(get("/orders/999").with(user("testuser")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void placeOrder_shouldHandleInsufficientStock() throws Exception {
        TestDelayUtil.smallDelay();
        // Given
        when(orderService.createOrder(any(User.class), anyString()))
                .thenThrow(new IllegalStateException("Insufficient stock"));

        // When & Then
        mockMvc.perform(post("/orders/place")
                        .with(csrf()).with(user("testuser"))
                        .param("shippingAddress", "123 Test Street"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void placeOrder_shouldHandleEmptyCart() throws Exception {
        // Given
        when(orderService.createOrder(any(User.class), anyString()))
                .thenThrow(new IllegalStateException("Cannot create order from empty cart"));

        // When & Then
        mockMvc.perform(post("/orders/place")
                        .with(csrf()).with(user("testuser"))
                        .param("shippingAddress", "123 Test Street"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void listOrders_shouldShowEmptyListWhenNoOrders() throws Exception {
        // Given
        Page<Order> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);
        when(orderService.findUserOrders(any(User.class), any())).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/orders").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"))
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    void checkout_shouldRequireAuthentication() throws Exception {
        // Given - Simulate empty cart
        when(cartService.getTotalItems()).thenReturn(0);

        // When & Then
        mockMvc.perform(get("/orders/checkout"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "testuser")
    void placeOrder_shouldValidateAddressLength() throws Exception {
        // Given - Very long address
        String longAddress = "A".repeat(500);

        // When & Then
        mockMvc.perform(post("/orders/place")
                        .with(csrf()).with(user("testuser"))
                        .param("shippingAddress", longAddress))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/checkout"));

        verify(orderService, never()).createOrder(any(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    void orderHistory_shouldDisplayUserOrderHistory() throws Exception {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 1);
        when(orderService.findUserOrders(any(User.class), any())).thenReturn(orderPage);

        // When & Then
        mockMvc.perform(get("/orders/history").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/list"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attributeExists("currentPage"));

        verify(orderService).findUserOrders(any(User.class), any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void cancelOrder_shouldReturn302AndRedirect_whenSuccessful() throws Exception {
        TestDelayUtil.smallDelay();
        // Given
        testOrder.setStatus(Order.OrderStatus.CANCELLED);
        when(orderService.cancelOrder(eq(1L), any(User.class), isNull()))
                .thenReturn(testOrder);

        // When & Then
        mockMvc.perform(post("/orders/1/cancel")
                        .with(csrf()).with(user("testuser")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"));

        verify(orderService).cancelOrder(eq(1L), any(User.class), isNull());
    }

    @Test
    @WithMockUser(username = "testuser")
    void cancelOrder_shouldReturnErrorMessage_whenOrderNotCancelable() throws Exception {
        TestDelayUtil.smallDelay();
        // Given
        when(orderService.cancelOrder(eq(1L), any(User.class), isNull()))
                .thenThrow(new InvalidOrderStateException(1L, Order.OrderStatus.SHIPPED,
                        "Order cannot be cancelled"));

        // When & Then
        mockMvc.perform(post("/orders/1/cancel")
                        .with(csrf()).with(user("testuser")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"));

        verify(orderService).cancelOrder(eq(1L), any(User.class), isNull());
    }

    @Test
    @WithMockUser(username = "testuser")
    void cancelOrder_shouldDenyAccess_whenUserDoesNotOwnOrder() throws Exception {
        TestDelayUtil.smallDelay();
        // Given
        when(orderService.cancelOrder(eq(1L), any(User.class), isNull()))
                .thenThrow(new InvalidOrderStateException("User does not own this order"));

        // When & Then
        mockMvc.perform(post("/orders/1/cancel")
                        .with(csrf()).with(user("testuser")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1"));

        verify(orderService).cancelOrder(eq(1L), any(User.class), isNull());
    }
}
