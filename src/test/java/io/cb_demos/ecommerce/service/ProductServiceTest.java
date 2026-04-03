package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.Category;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.exception.InsufficientStockException;
import io.cb_demos.ecommerce.exception.ProductNotFoundException;
import io.cb_demos.ecommerce.repository.CategoryRepository;
import io.cb_demos.ecommerce.repository.ProductRepository;
import io.cb_demos.ecommerce.service.impl.ProductServiceImpl;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Electronics");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStockQuantity(10);
        testProduct.setCategory(testCategory);
        testProduct.setActive(true);
    }

    @Test
    void findAllProducts_shouldReturnPageOfActiveProducts() {
        TestDelayUtil.mediumDelay();
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, pageable, products.size());

        when(productRepository.findByActiveTrue(pageable)).thenReturn(productPage);

        // When
        Page<Product> result = productService.findAllProducts(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Product");
        verify(productRepository).findByActiveTrue(pageable);
    }

    @Test
    void findById_shouldReturnProduct_whenProductExists() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        Product result = productService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Product");
        verify(productRepository).findById(1L);
    }

    @Test
    void findById_shouldThrowException_whenProductNotFound() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.findById(999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("999");
        verify(productRepository).findById(999L);
    }

    @Test
    void findByCategory_shouldReturnProductsInCategory() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, pageable, products.size());

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.findByCategoryAndActiveTrue(testCategory, pageable)).thenReturn(productPage);

        // When
        Page<Product> result = productService.findByCategory(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(categoryRepository).findById(1L);
        verify(productRepository).findByCategoryAndActiveTrue(testCategory, pageable);
    }

    @Test
    void searchProducts_shouldReturnMatchingProducts() {
        TestDelayUtil.mediumDelay();
        // Given
        String query = "Test";
        Pageable pageable = PageRequest.of(0, 10);
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, pageable, products.size());

        when(productRepository.searchProducts(query, pageable)).thenReturn(productPage);

        // When
        Page<Product> result = productService.searchProducts(query, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).searchProducts(query, pageable);
    }

    @Test
    void isInStock_shouldReturnTrue_whenSufficientStock() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        boolean result = productService.isInStock(1L, 5);

        // Then
        assertThat(result).isTrue();
        verify(productRepository).findById(1L);
    }

    @Test
    void isInStock_shouldReturnFalse_whenInsufficientStock() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        boolean result = productService.isInStock(1L, 20);

        // Then
        assertThat(result).isFalse();
        verify(productRepository).findById(1L);
    }

    @Test
    void updateStock_shouldIncreaseStock_whenPositiveQuantity() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        productService.updateStock(1L, 5);

        // Then
        verify(productRepository).findById(1L);
        verify(productRepository).save(testProduct);
        assertThat(testProduct.getStockQuantity()).isEqualTo(15);
    }

    @Test
    void updateStock_shouldDecreaseStock_whenNegativeQuantity() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        productService.updateStock(1L, -3);

        // Then
        verify(productRepository).findById(1L);
        verify(productRepository).save(testProduct);
        assertThat(testProduct.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void updateStock_shouldThrowException_whenResultingStockIsNegative() {
        TestDelayUtil.mediumDelay();
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When & Then
        assertThatThrownBy(() -> productService.updateStock(1L, -15))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Test Product");
        verify(productRepository).findById(1L);
        verify(productRepository, never()).save(any());
    }

    @Test
    void findFeaturedProducts_shouldReturnTop8Products() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        when(productRepository.findTop8ByActiveTrueOrderByCreatedAtDesc()).thenReturn(products);

        // When
        List<Product> result = productService.findFeaturedProducts();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(productRepository).findTop8ByActiveTrueOrderByCreatedAtDesc();
    }
}
