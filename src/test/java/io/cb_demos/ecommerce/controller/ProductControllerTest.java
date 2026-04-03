package io.cb_demos.ecommerce.controller;
import io.cb_demos.ecommerce.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;

import io.cb_demos.ecommerce.domain.Category;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.service.CategoryService;
import io.cb_demos.ecommerce.service.ProductService;
import io.cb_demos.ecommerce.service.ProductReviewService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestSecurityConfig.class)
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private ProductReviewService reviewService;

    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Electronics");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Laptop");
        testProduct.setDescription("High-performance laptop");
        testProduct.setPrice(new BigDecimal("999.99"));
        testProduct.setStockQuantity(5);
        testProduct.setCategory(testCategory);
        testProduct.setActive(true);
    }

    @Test
    @WithMockUser
    void listProducts_shouldReturnProductsPage() throws Exception {
        TestDelayUtil.smallDelay();
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 12), 1);
        when(productService.findAllProducts(any())).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("currentPage"));
    }

    @Test
    @WithMockUser
    void listProducts_shouldHandlePagination() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(1, 12), 25);
        when(productService.findAllProducts(any())).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/products")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"))
                .andExpect(model().attribute("currentPage", 1))
                .andExpect(model().attribute("totalPages", 3));
    }

    @Test
    @WithMockUser
    void productDetail_shouldReturnProductDetails() throws Exception {
        TestDelayUtil.smallDelay();
        // Given
        when(productService.findById(1L)).thenReturn(testProduct);
        when(reviewService.getReviewsByProduct(1L)).thenReturn(Collections.emptyList());
        when(reviewService.getAverageRating(1L)).thenReturn(0.0);
        when(reviewService.getReviewCount(1L)).thenReturn(0L);

        // When & Then
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/detail"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attribute("product", testProduct));
    }

    @Test
    @WithMockUser
    void productsByCategory_shouldReturnFilteredProducts() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 12), 1);
        when(categoryService.findById(1L)).thenReturn(testCategory);
        when(productService.findByCategory(eq(1L), any())).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/products/category/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("category"))
                .andExpect(model().attribute("category", testCategory));
    }

    @Test
    @WithMockUser
    void searchProducts_shouldReturnMatchingProducts() throws Exception {
        // Given
        String searchQuery = "laptop";
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 12), 1);
        when(productService.searchProducts(eq(searchQuery), any())).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/products/search")
                        .param("query", searchQuery))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("searchQuery"))
                .andExpect(model().attribute("searchQuery", searchQuery));
    }

    @Test
    @WithMockUser
    void searchProducts_shouldHandleEmptyQuery() throws Exception {
        // When & Then
        mockMvc.perform(get("/products/search")
                        .param("query", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }

    @Test
    @WithMockUser
    void productDetail_shouldReturnStockAvailability() throws Exception {
        // Given
        testProduct.setStockQuantity(0);
        when(productService.findById(1L)).thenReturn(testProduct);
        when(reviewService.getReviewsByProduct(1L)).thenReturn(Collections.emptyList());
        when(reviewService.getAverageRating(1L)).thenReturn(0.0);
        when(reviewService.getReviewCount(1L)).thenReturn(0L);

        // When & Then
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/detail"))
                .andExpect(model().attributeExists("product"));
    }

    @Test
    @WithMockUser
    void listProducts_shouldHandleEmptyResults() throws Exception {
        // Given
        Page<Product> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 12), 0);
        when(productService.findAllProducts(any())).thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"))
                .andExpect(model().attributeExists("products"));
    }

    @Test
    @WithMockUser
    void productsByCategory_shouldHandleInvalidCategoryGracefully() throws Exception {
        // Given
        when(categoryService.findById(999L)).thenThrow(new IllegalArgumentException("Category not found"));

        // When & Then
        mockMvc.perform(get("/products/category/999"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    void searchProducts_shouldHandleSpecialCharacters() throws Exception {
        // Given
        String searchQuery = "laptop & tablets";
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 12), 1);
        when(productService.searchProducts(eq(searchQuery), any())).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/products/search")
                        .param("query", searchQuery))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"));
    }
}
