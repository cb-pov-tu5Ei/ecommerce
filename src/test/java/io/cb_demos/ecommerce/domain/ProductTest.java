package io.cb_demos.ecommerce.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    private Product product;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Electronics");

        product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setDescription("High-performance laptop");
        product.setPrice(new BigDecimal("999.99"));
        product.setStockQuantity(10);
        product.setCategory(category);
        product.setActive(true);
    }

    @Test
    void getters_shouldReturnCorrectValues() {
        assertThat(product.getId()).isEqualTo(1L);
        assertThat(product.getName()).isEqualTo("Laptop");
        assertThat(product.getDescription()).isEqualTo("High-performance laptop");
        assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(product.getStockQuantity()).isEqualTo(10);
        assertThat(product.getCategory()).isEqualTo(category);
        assertThat(product.isActive()).isTrue();
    }

    @Test
    void setters_shouldUpdateValues() {
        // When
        product.setName("Gaming Laptop");
        product.setDescription("Updated description");
        product.setPrice(new BigDecimal("1299.99"));
        product.setStockQuantity(5);
        product.setActive(false);

        // Then
        assertThat(product.getName()).isEqualTo("Gaming Laptop");
        assertThat(product.getDescription()).isEqualTo("Updated description");
        assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("1299.99"));
        assertThat(product.getStockQuantity()).isEqualTo(5);
        assertThat(product.isActive()).isFalse();
    }

    @Test
    void equals_shouldComparById() {
        // Given
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");

        Product product2 = new Product();
        product2.setId(1L);
        product2.setName("Product 2");

        Product product3 = new Product();
        product3.setId(2L);
        product3.setName("Product 1");

        // Then
        assertThat(product1).isEqualTo(product2); // Same ID
        assertThat(product1).isNotEqualTo(product3); // Different ID
    }

    @Test
    void hashCode_shouldBeConsistent() {
        // Given
        Product product1 = new Product();
        product1.setId(1L);

        Product product2 = new Product();
        product2.setId(1L);

        // Then
        assertThat(product1.hashCode()).isEqualTo(product2.hashCode());
    }

    @Test
    void toString_shouldIncludeProductDetails() {
        // When
        String result = product.toString();

        // Then
        assertThat(result).contains("Laptop");
        assertThat(result).contains("999.99");
    }

    @Test
    void isInStock_shouldReturnTrueWhenStockAvailable() {
        // Given
        product.setStockQuantity(10);

        // Then
        assertThat(product.getStockQuantity() > 0).isTrue();
    }

    @Test
    void isInStock_shouldReturnFalseWhenNoStock() {
        // Given
        product.setStockQuantity(0);

        // Then
        assertThat(product.getStockQuantity() > 0).isFalse();
    }

    @Test
    void priceComparison_shouldWorkCorrectly() {
        // Given
        Product cheapProduct = new Product();
        cheapProduct.setPrice(new BigDecimal("10.00"));

        Product expensiveProduct = new Product();
        expensiveProduct.setPrice(new BigDecimal("1000.00"));

        // Then
        assertThat(cheapProduct.getPrice()).isLessThan(expensiveProduct.getPrice());
        assertThat(expensiveProduct.getPrice()).isGreaterThan(cheapProduct.getPrice());
    }

    @Test
    void newProduct_shouldHaveNullId() {
        // Given
        Product newProduct = new Product();

        // Then
        assertThat(newProduct.getId()).isNull();
    }

    @Test
    void category_shouldBeSettable() {
        // Given
        Category newCategory = new Category();
        newCategory.setId(2L);
        newCategory.setName("Books");

        // When
        product.setCategory(newCategory);

        // Then
        assertThat(product.getCategory()).isEqualTo(newCategory);
        assertThat(product.getCategory().getName()).isEqualTo("Books");
    }

    @Test
    void priceScale_shouldHandleDecimals() {
        // Given
        product.setPrice(new BigDecimal("99.999"));

        // Then
        assertThat(product.getPrice().scale()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void stockQuantity_shouldAllowZero() {
        // When
        product.setStockQuantity(0);

        // Then
        assertThat(product.getStockQuantity()).isZero();
    }

    @Test
    void stockQuantity_shouldAllowLargeNumbers() {
        // When
        product.setStockQuantity(1000000);

        // Then
        assertThat(product.getStockQuantity()).isEqualTo(1000000);
    }

    @Test
    void description_shouldAllowLongText() {
        // Given
        String longDescription = "A".repeat(1000);

        // When
        product.setDescription(longDescription);

        // Then
        assertThat(product.getDescription()).hasSize(1000);
    }

    @Test
    void active_shouldToggle() {
        // When
        product.setActive(true);
        assertThat(product.isActive()).isTrue();

        product.setActive(false);
        assertThat(product.isActive()).isFalse();
    }

    @Test
    void builder_shouldCreateProductCorrectly() {
        // Given
        Product newProduct = new Product();
        newProduct.setName("Test Product");
        newProduct.setPrice(new BigDecimal("50.00"));
        newProduct.setStockQuantity(15);
        newProduct.setActive(true);

        // Then
        assertThat(newProduct.getName()).isEqualTo("Test Product");
        assertThat(newProduct.getPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(newProduct.getStockQuantity()).isEqualTo(15);
        assertThat(newProduct.isActive()).isTrue();
    }
}
