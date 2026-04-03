package io.cb_demos.ecommerce.controller;

import io.cb_demos.ecommerce.config.TestSecurityConfig;
import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.exception.MinimumOrderValueException;
import io.cb_demos.ecommerce.service.CartService;
import io.cb_demos.ecommerce.service.OrderService;
import io.cb_demos.ecommerce.service.UserService;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
class MinimumOrderValueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser
    void shouldRedirectToCartWithErrorWhenMinimumNotMet() throws Exception {
        TestDelayUtil.mediumDelay();

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        when(cartService.getTotalItems()).thenReturn(1);
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(orderService.createOrder(any(User.class), anyString()))
                .thenThrow(new MinimumOrderValueException(
                        new BigDecimal("10.00"),
                        new BigDecimal("20.00")
                ));

        mockMvc.perform(post("/orders/place")
                        .with(csrf())
                        .param("shippingAddress", "123 Test St"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("error",
                        "Order total of $10.00 does not meet minimum order value of $20.00. Add $10.00 more to proceed."));
    }

    @Test
    @WithMockUser
    void shouldDisplayHelpfulErrorMessageWithAmountNeeded() throws Exception {
        TestDelayUtil.mediumDelay();

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        when(cartService.getTotalItems()).thenReturn(2);
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(orderService.createOrder(any(User.class), anyString()))
                .thenThrow(new MinimumOrderValueException(
                        new BigDecimal("15.50"),
                        new BigDecimal("20.00")
                ));

        mockMvc.perform(post("/orders/place")
                        .with(csrf())
                        .param("shippingAddress", "456 Main Ave"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attributeExists("error"))
                .andExpect(flash().attribute("error",
                        org.hamcrest.Matchers.containsString("$15.50")))
                .andExpect(flash().attribute("error",
                        org.hamcrest.Matchers.containsString("$20.00")))
                .andExpect(flash().attribute("error",
                        org.hamcrest.Matchers.containsString("$4.50")));
    }

    @Test
    @WithMockUser
    void shouldHandleMinimumOrderExceptionWithPennies() throws Exception {
        TestDelayUtil.mediumDelay();

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        when(cartService.getTotalItems()).thenReturn(1);
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(orderService.createOrder(any(User.class), anyString()))
                .thenThrow(new MinimumOrderValueException(
                        new BigDecimal("19.99"),
                        new BigDecimal("20.00")
                ));

        mockMvc.perform(post("/orders/place")
                        .with(csrf())
                        .param("shippingAddress", "789 Oak Ln"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error",
                        org.hamcrest.Matchers.containsString("$0.01")));
    }
}
