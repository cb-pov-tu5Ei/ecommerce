package io.cb_demos.ecommerce.controller;
import io.cb_demos.ecommerce.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;

import io.cb_demos.ecommerce.domain.CartItem;
import io.cb_demos.ecommerce.service.CartService;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestSecurityConfig.class)
@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testCartItem = new CartItem();
        testCartItem.setProductId(1L);
        testCartItem.setProductName("Test Product");
        testCartItem.setPrice(new BigDecimal("29.99"));
        testCartItem.setQuantity(2);
    }

    @Test
    @WithMockUser
    void viewCart_shouldDisplayCartItems() throws Exception {
        TestDelayUtil.smallDelay();
        // Given
        List<CartItem> cartItems = Arrays.asList(testCartItem);
        when(cartService.getCartItems()).thenReturn(cartItems);
        when(cartService.calculateTotal()).thenReturn(new BigDecimal("59.98"));
        when(cartService.getTotalItems()).thenReturn(2);

        // When & Then
        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/view"))
                .andExpect(model().attributeExists("cartItems"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("total", new BigDecimal("59.98")));

        verify(cartService).getCartItems();
        verify(cartService).calculateTotal();
    }

    @Test
    @WithMockUser
    void viewCart_shouldDisplayEmptyCart() throws Exception {
        // Given
        when(cartService.getCartItems()).thenReturn(Arrays.asList());
        when(cartService.calculateTotal()).thenReturn(BigDecimal.ZERO);
        when(cartService.getTotalItems()).thenReturn(0);

        // When & Then
        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/view"))
                .andExpect(model().attribute("total", BigDecimal.ZERO));
    }

    @Test
    @WithMockUser
    void addToCart_shouldAddItemAndRedirect() throws Exception {
        TestDelayUtil.smallDelay();
        // Given
        doNothing().when(cartService).addToCart(1L, 1);

        // When & Then
        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .param("productId", "1")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService).addToCart(1L, 1);
    }

    @Test
    @WithMockUser
    void addToCart_shouldAddMultipleQuantity() throws Exception {
        // Given
        doNothing().when(cartService).addToCart(1L, 5);

        // When & Then
        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .param("productId", "1")
                        .param("quantity", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService).addToCart(1L, 5);
    }

    @Test
    @WithMockUser
    void updateQuantity_shouldUpdateItemQuantity() throws Exception {
        // Given
        doNothing().when(cartService).updateQuantity(1L, 3);

        // When & Then
        mockMvc.perform(post("/cart/update")
                        .with(csrf())
                        .param("productId", "1")
                        .param("quantity", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService).updateQuantity(1L, 3);
    }

    @Test
    @WithMockUser
    void updateQuantity_shouldRemoveItemWhenQuantityZero() throws Exception {
        // Given
        doNothing().when(cartService).updateQuantity(1L, 0);

        // When & Then
        mockMvc.perform(post("/cart/update")
                        .with(csrf())
                        .param("productId", "1")
                        .param("quantity", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService).updateQuantity(1L, 0);
    }

    @Test
    @WithMockUser
    void removeFromCart_shouldRemoveItemAndRedirect() throws Exception {
        // Given
        doNothing().when(cartService).removeFromCart(1L);

        // When & Then
        mockMvc.perform(post("/cart/remove/1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService).removeFromCart(1L);
    }

    @Test
    @WithMockUser
    void clearCart_shouldClearAllItemsAndRedirect() throws Exception {
        // Given
        doNothing().when(cartService).clearCart();

        // When & Then
        mockMvc.perform(post("/cart/clear")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        verify(cartService).clearCart();
    }

    @Test
    @WithMockUser
    void addToCart_shouldHandleInvalidProductId() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Product not found"))
                .when(cartService).addToCart(999L, 1);

        // When & Then
        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .param("productId", "999")
                        .param("quantity", "1"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    void addToCart_shouldHandleNegativeQuantity() throws Exception {
        // When & Then
        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .param("productId", "1")
                        .param("quantity", "-1"))
                .andExpect(status().is4xxClientError());

        verify(cartService, never()).addToCart(anyLong(), anyInt());
    }

    @Test
    @WithMockUser
    void viewCart_shouldShowCorrectTotalForMultipleItems() throws Exception {
        // Given
        CartItem secondItem = new CartItem();
        secondItem.setProductId(2L);
        secondItem.setProductName("Second Product");
        secondItem.setPrice(new BigDecimal("49.99"));
        secondItem.setQuantity(1);

        List<CartItem> cartItems = Arrays.asList(testCartItem, secondItem);
        when(cartService.getCartItems()).thenReturn(cartItems);
        when(cartService.calculateTotal()).thenReturn(new BigDecimal("109.97"));
        when(cartService.getTotalItems()).thenReturn(3);

        // When & Then
        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("total", new BigDecimal("109.97")));
    }

    @Test
    void addToCart_shouldRequireAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .param("productId", "1")
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection());
    }
}
