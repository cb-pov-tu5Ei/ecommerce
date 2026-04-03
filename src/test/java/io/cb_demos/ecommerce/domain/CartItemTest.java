package io.cb_demos.ecommerce.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CartItemTest {

    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        cartItem = new CartItem();
        cartItem.setProductId(1L);
        cartItem.setProductName("Test Product");
        cartItem.setPrice(new BigDecimal("29.99"));
        cartItem.setQuantity(2);
        cartItem.setImageUrl("/images/test.jpg");
    }

    @Test
    void getters_shouldReturnCorrectValues() {
        assertThat(cartItem.getProductId()).isEqualTo(1L);
        assertThat(cartItem.getProductName()).isEqualTo("Test Product");
        assertThat(cartItem.getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
        assertThat(cartItem.getQuantity()).isEqualTo(2);
        assertThat(cartItem.getImageUrl()).isEqualTo("/images/test.jpg");
    }

    @Test
    void setters_shouldUpdateValues() {
        // When
        cartItem.setProductId(2L);
        cartItem.setProductName("Updated Product");
        cartItem.setPrice(new BigDecimal("39.99"));
        cartItem.setQuantity(5);
        cartItem.setImageUrl("/images/updated.jpg");

        // Then
        assertThat(cartItem.getProductId()).isEqualTo(2L);
        assertThat(cartItem.getProductName()).isEqualTo("Updated Product");
        assertThat(cartItem.getPrice()).isEqualByComparingTo(new BigDecimal("39.99"));
        assertThat(cartItem.getQuantity()).isEqualTo(5);
        assertThat(cartItem.getImageUrl()).isEqualTo("/images/updated.jpg");
    }

    @Test
    void getSubtotal_shouldCalculateCorrectly() {
        // When
        BigDecimal subtotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        // Then
        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("59.98"));
    }

    @Test
    void getSubtotal_shouldHandleSingleItem() {
        // Given
        cartItem.setQuantity(1);
        cartItem.setPrice(new BigDecimal("10.00"));

        // When
        BigDecimal subtotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        // Then
        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void getSubtotal_shouldHandleLargeQuantity() {
        // Given
        cartItem.setQuantity(100);
        cartItem.setPrice(new BigDecimal("5.99"));

        // When
        BigDecimal subtotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        // Then
        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("599.00"));
    }

    @Test
    void quantity_shouldBePositive() {
        // When
        cartItem.setQuantity(5);

        // Then
        assertThat(cartItem.getQuantity()).isPositive();
    }

    @Test
    void quantity_shouldAllowOne() {
        // When
        cartItem.setQuantity(1);

        // Then
        assertThat(cartItem.getQuantity()).isEqualTo(1);
    }

    @Test
    void price_shouldHandleDecimals() {
        // When
        cartItem.setPrice(new BigDecimal("9.99"));

        // Then
        assertThat(cartItem.getPrice()).isEqualByComparingTo(new BigDecimal("9.99"));
    }

    @Test
    void price_shouldHandleLargeAmounts() {
        // When
        cartItem.setPrice(new BigDecimal("9999.99"));

        // Then
        assertThat(cartItem.getPrice()).isEqualByComparingTo(new BigDecimal("9999.99"));
    }

    @Test
    void productName_shouldAllowLongNames() {
        // Given
        String longName = "This is a very long product name that contains many words and characters";

        // When
        cartItem.setProductName(longName);

        // Then
        assertThat(cartItem.getProductName()).isEqualTo(longName);
    }

    @Test
    void imageUrl_shouldAcceptRelativePaths() {
        // When
        cartItem.setImageUrl("/images/products/item.jpg");

        // Then
        assertThat(cartItem.getImageUrl()).isEqualTo("/images/products/item.jpg");
    }

    @Test
    void imageUrl_shouldAcceptAbsolutePaths() {
        // When
        cartItem.setImageUrl("https://example.com/images/product.jpg");

        // Then
        assertThat(cartItem.getImageUrl()).isEqualTo("https://example.com/images/product.jpg");
    }

    @Test
    void imageUrl_shouldAllowNull() {
        // When
        cartItem.setImageUrl(null);

        // Then
        assertThat(cartItem.getImageUrl()).isNull();
    }

    @Test
    void newCartItem_shouldHaveDefaultValues() {
        // Given
        CartItem newItem = new CartItem();

        // Then
        assertThat(newItem.getProductId()).isNull();
        assertThat(newItem.getProductName()).isNull();
        assertThat(newItem.getPrice()).isNull();
        assertThat(newItem.getQuantity()).isZero();
    }

    @Test
    void toString_shouldIncludeItemDetails() {
        // When
        String result = cartItem.toString();

        // Then
        assertThat(result).contains("Test Product");
    }

    @Test
    void equals_shouldCompareByProductId() {
        // Given
        CartItem item1 = new CartItem();
        item1.setProductId(1L);
        item1.setProductName("Product 1");

        CartItem item2 = new CartItem();
        item2.setProductId(1L);
        item2.setProductName("Product 2");

        CartItem item3 = new CartItem();
        item3.setProductId(2L);
        item3.setProductName("Product 1");

        // Then
        assertThat(item1.getProductId()).isEqualTo(item2.getProductId());
        assertThat(item1.getProductId()).isNotEqualTo(item3.getProductId());
    }

    @Test
    void priceMultiplication_shouldMaintainPrecision() {
        // Given
        cartItem.setPrice(new BigDecimal("10.99"));
        cartItem.setQuantity(3);

        // When
        BigDecimal total = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        // Then
        assertThat(total).isEqualByComparingTo(new BigDecimal("32.97"));
    }

    @Test
    void incrementQuantity_shouldWork() {
        // Given
        int initialQuantity = cartItem.getQuantity();

        // When
        cartItem.setQuantity(cartItem.getQuantity() + 1);

        // Then
        assertThat(cartItem.getQuantity()).isEqualTo(initialQuantity + 1);
    }

    @Test
    void decrementQuantity_shouldWork() {
        // Given
        cartItem.setQuantity(5);

        // When
        cartItem.setQuantity(cartItem.getQuantity() - 1);

        // Then
        assertThat(cartItem.getQuantity()).isEqualTo(4);
    }

    @Test
    void productName_shouldAllowSpecialCharacters() {
        // Given
        String specialName = "Product & Item (Special) - 50% Off!";

        // When
        cartItem.setProductName(specialName);

        // Then
        assertThat(cartItem.getProductName()).isEqualTo(specialName);
    }

    @Test
    void price_shouldHandleCents() {
        // When
        cartItem.setPrice(new BigDecimal("0.99"));

        // Then
        assertThat(cartItem.getPrice()).isEqualByComparingTo(new BigDecimal("0.99"));
    }

    @Test
    void subtotal_shouldBeZeroForZeroQuantity() {
        // Given
        cartItem.setQuantity(0);
        cartItem.setPrice(new BigDecimal("50.00"));

        // When
        BigDecimal subtotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        // Then
        assertThat(subtotal).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
