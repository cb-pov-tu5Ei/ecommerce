package io.cb_demos.ecommerce.controller;
import io.cb_demos.ecommerce.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;

import io.cb_demos.ecommerce.domain.Category;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.service.CategoryService;
import io.cb_demos.ecommerce.service.ProductService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestSecurityConfig.class)
@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    private List<Product> featuredProducts;
    private List<Category> categories;

    @BeforeEach
    void setUp() {
        Category electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");

        Category clothing = new Category();
        clothing.setId(2L);
        clothing.setName("Clothing");

        categories = Arrays.asList(electronics, clothing);

        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Laptop");
        product1.setPrice(new BigDecimal("999.99"));
        product1.setCategory(electronics);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("T-Shirt");
        product2.setPrice(new BigDecimal("19.99"));
        product2.setCategory(clothing);

        featuredProducts = Arrays.asList(product1, product2);
    }

    @Test
    @WithMockUser
    void home_shouldDisplayFeaturedProductsAndCategories() throws Exception {
        // Given
        when(productService.findFeaturedProducts()).thenReturn(featuredProducts);
        when(categoryService.findAllCategories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("featuredProducts"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attribute("featuredProducts", featuredProducts))
                .andExpect(model().attribute("categories", categories));

        verify(productService).findFeaturedProducts();
        verify(categoryService).findAllCategories();
    }

    @Test
    @WithMockUser
    void home_shouldHandleEmptyFeaturedProducts() throws Exception {
        // Given
        when(productService.findFeaturedProducts()).thenReturn(Arrays.asList());
        when(categoryService.findAllCategories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("featuredProducts"))
                .andExpect(model().attribute("featuredProducts", Arrays.asList()));
    }

    @Test
    @WithMockUser
    void home_shouldHandleEmptyCategories() throws Exception {
        // Given
        when(productService.findFeaturedProducts()).thenReturn(featuredProducts);
        when(categoryService.findAllCategories()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attribute("categories", Arrays.asList()));
    }

    @Test
    @WithMockUser
    void home_shouldCallServicesOnce() throws Exception {
        // Given
        when(productService.findFeaturedProducts()).thenReturn(featuredProducts);
        when(categoryService.findAllCategories()).thenReturn(categories);

        // When
        mockMvc.perform(get("/"));

        // Then
        verify(productService, times(1)).findFeaturedProducts();
        verify(categoryService, times(1)).findAllCategories();
    }

    @Test
    @WithMockUser
    void home_shouldHandleServiceExceptions() throws Exception {
        // Given
        when(productService.findFeaturedProducts()).thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void home_shouldBeAccessibleWithoutAuthentication() throws Exception {
        // Given
        when(productService.findFeaturedProducts()).thenReturn(featuredProducts);
        when(categoryService.findAllCategories()).thenReturn(categories);

        // When & Then - No @WithMockUser annotation
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    @Test
    @WithMockUser
    void home_shouldHandleLargeFeaturedProductList() throws Exception {
        // Given - Create 100 products
        List<Product> manyProducts = new java.util.ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            Product p = new Product();
            p.setId((long) i);
            p.setName("Product " + i);
            p.setPrice(new BigDecimal("10.00"));
            manyProducts.add(p);
        }

        when(productService.findFeaturedProducts()).thenReturn(manyProducts);
        when(categoryService.findAllCategories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("featuredProducts", manyProducts));
    }

    @Test
    @WithMockUser
    void about_shouldReturnAboutPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(view().name("about"));
    }

    @Test
    @WithMockUser
    void contact_shouldReturnContactPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/contact"))
                .andExpect(status().isOk())
                .andExpect(view().name("contact"));
    }

    @Test
    void about_shouldBeAccessibleWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/about"))
                .andExpect(status().isOk());
    }

    @Test
    void contact_shouldBeAccessibleWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/contact"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void home_shouldHandleNullFeaturedProducts() throws Exception {
        // Given
        when(productService.findFeaturedProducts()).thenReturn(null);
        when(categoryService.findAllCategories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser
    void homeIndex_shouldRedirectToHome() throws Exception {
        // Given
        when(productService.findFeaturedProducts()).thenReturn(featuredProducts);
        when(categoryService.findAllCategories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }
}
