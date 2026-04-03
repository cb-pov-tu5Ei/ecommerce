package io.cb_demos.ecommerce.exception;

import io.cb_demos.ecommerce.controller.ProductController;
import io.cb_demos.ecommerce.service.CategoryService;
import io.cb_demos.ecommerce.service.ProductService;
import io.cb_demos.ecommerce.service.ProductReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@ContextConfiguration(classes = {ProductController.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private ProductReviewService reviewService;

    @Test
    @WithMockUser
    void handleProductNotFoundException_shouldReturn404() throws Exception {
        // Given
        when(productService.findById(anyLong()))
                .thenThrow(new ProductNotFoundException("Product not found with id: 999"));

        // When & Then
        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "Product not found with id: 999"));
    }

    @Test
    @WithMockUser
    void handleInsufficientStockException_shouldReturn400() throws Exception {
        // Given
        when(productService.findById(anyLong()))
                .thenThrow(new InsufficientStockException("Not enough stock for product: Laptop"));

        // When & Then
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "Not enough stock for product: Laptop"));
    }

    @Test
    @WithMockUser
    void handleOrderNotFoundException_shouldReturn404() throws Exception {
        // Given
        when(productService.findById(anyLong()))
                .thenThrow(new OrderNotFoundException("Order not found with id: 123"));

        // When & Then
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser
    void handleIllegalArgumentException_shouldReturn400() throws Exception {
        // Given
        when(productService.findById(anyLong()))
                .thenThrow(new IllegalArgumentException("Invalid product ID"));

        // When & Then
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser
    void handleIllegalStateException_shouldReturn400() throws Exception {
        // Given
        when(productService.findById(anyLong()))
                .thenThrow(new IllegalStateException("Cannot process request"));

        // When & Then
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/400"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser
    void handleGenericException_shouldReturn500() throws Exception {
        // Given
        when(productService.findById(anyLong()))
                .thenThrow(new RuntimeException("Unexpected error occurred"));

        // When & Then
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser
    void handleNullPointerException_shouldReturn500() throws Exception {
        // Given
        when(productService.findById(anyLong()))
                .thenThrow(new NullPointerException("Null value encountered"));

        // When & Then
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error/500"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser
    void handleProductNotFoundException_shouldIncludeErrorMessage() throws Exception {
        // Given
        String errorMessage = "Product with ID 999 was not found in the database";
        when(productService.findById(999L))
                .thenThrow(new ProductNotFoundException(errorMessage));

        // When & Then
        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(model().attribute("error", errorMessage));
    }

    @Test
    @WithMockUser
    void handleInsufficientStockException_shouldIncludeProductDetails() throws Exception {
        // Given
        String errorMessage = "Insufficient stock for product: Gaming Laptop. Requested: 10, Available: 3";
        when(productService.findById(1L))
                .thenThrow(new InsufficientStockException(errorMessage));

        // When & Then
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute("error", errorMessage));
    }

    @Test
    @WithMockUser
    void handleMultipleExceptions_shouldHandleCorrectly() throws Exception {
        // Test that different endpoints can handle different exceptions

        // First exception
        when(productService.findById(1L))
                .thenThrow(new ProductNotFoundException("Product 1 not found"));

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isNotFound());

        // Second exception
        when(productService.findById(2L))
                .thenThrow(new InsufficientStockException("No stock for product 2"));

        mockMvc.perform(get("/products/2"))
                .andExpect(status().isBadRequest());
    }
}
