package io.cb_demos.ecommerce.integration;

import io.cb_demos.ecommerce.domain.CartItem;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.repository.ProductRepository;
import io.cb_demos.ecommerce.service.CartService;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.BeforeEach;
import io.cb_demos.ecommerce.exception.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CartManagementIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        product1 = new Product();
        product1.setName("Laptop");
        product1.setDescription("Gaming laptop");
        product1.setPrice(new BigDecimal("1299.99"));
        product1.setStockQuantity(10);
        product1.setActive(true);
        product1 = productRepository.save(product1);

        product2 = new Product();
        product2.setName("Mouse");
        product2.setDescription("Wireless mouse");
        product2.setPrice(new BigDecimal("29.99"));
        product2.setStockQuantity(50);
        product2.setActive(true);
        product2 = productRepository.save(product2);

        product3 = new Product();
        product3.setName("Keyboard");
        product3.setDescription("Mechanical keyboard");
        product3.setPrice(new BigDecimal("89.99"));
        product3.setStockQuantity(25);
        product3.setActive(true);
        product3 = productRepository.save(product3);

        cartService.clearCart();
    }

    @Test
    void addToCart_shouldAddSingleProduct() {
        TestDelayUtil.mediumDelay();
        // When
        cartService.addToCart(product1.getId(), 1);

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getProductId()).isEqualTo(product1.getId());
        assertThat(items.get(0).getProductName()).isEqualTo("Laptop");
        assertThat(items.get(0).getQuantity()).isEqualTo(1);
        assertThat(items.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("1299.99"));
    }

    @Test
    void addToCart_shouldAddMultipleProducts() {
        TestDelayUtil.massiveDelay();
        // When
        cartService.addToCart(product1.getId(), 1);
        cartService.addToCart(product2.getId(), 2);
        cartService.addToCart(product3.getId(), 3);

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items).hasSize(3);
        assertThat(cartService.getTotalItems()).isEqualTo(6);
    }

    @Test
    void addToCart_shouldIncrementQuantityForExistingProduct() {
        TestDelayUtil.mediumDelay();
        // When
        cartService.addToCart(product1.getId(), 2);
        cartService.addToCart(product1.getId(), 3);

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void calculateTotal_shouldReturnCorrectAmount() {
        TestDelayUtil.massiveDelay();
        // Given
        cartService.addToCart(product1.getId(), 1); // 1299.99
        cartService.addToCart(product2.getId(), 2); // 59.98
        cartService.addToCart(product3.getId(), 1); // 89.99

        // When
        BigDecimal total = cartService.calculateTotal();

        // Then
        BigDecimal expected = new BigDecimal("1299.99")
                .add(new BigDecimal("29.99").multiply(BigDecimal.valueOf(2)))
                .add(new BigDecimal("89.99"));
        assertThat(total).isEqualByComparingTo(expected);
    }

    @Test
    void calculateTotal_shouldReturnZeroForEmptyCart() {
        // When
        BigDecimal total = cartService.calculateTotal();

        // Then
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void updateQuantity_shouldModifyItemQuantity() {
        TestDelayUtil.mediumDelay();
        // Given
        cartService.addToCart(product1.getId(), 2);

        // When
        cartService.updateQuantity(product1.getId(), 5);

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items.get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void updateQuantity_shouldRemoveItemWhenSetToZero() {
        // Given
        cartService.addToCart(product1.getId(), 2);
        cartService.addToCart(product2.getId(), 1);

        // When
        cartService.updateQuantity(product1.getId(), 0);

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getProductId()).isEqualTo(product2.getId());
    }

    @Test
    void removeFromCart_shouldRemoveSpecificItem() {
        TestDelayUtil.mediumDelay();
        // Given
        cartService.addToCart(product1.getId(), 1);
        cartService.addToCart(product2.getId(), 1);
        cartService.addToCart(product3.getId(), 1);

        // When
        cartService.removeFromCart(product2.getId());

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items).hasSize(2);
        assertThat(items).noneMatch(item -> item.getProductId().equals(product2.getId()));
    }

    @Test
    void clearCart_shouldRemoveAllItems() {
        TestDelayUtil.mediumDelay();
        // Given
        cartService.addToCart(product1.getId(), 1);
        cartService.addToCart(product2.getId(), 1);
        cartService.addToCart(product3.getId(), 1);
        assertThat(cartService.getCartItems()).hasSize(3);

        // When
        cartService.clearCart();

        // Then
        assertThat(cartService.getCartItems()).isEmpty();
        assertThat(cartService.getTotalItems()).isZero();
        assertThat(cartService.calculateTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getTotalItems_shouldCountAllItemsInCart() {
        TestDelayUtil.mediumDelay();
        // Given
        cartService.addToCart(product1.getId(), 2);
        cartService.addToCart(product2.getId(), 3);
        cartService.addToCart(product3.getId(), 5);

        // When
        int total = cartService.getTotalItems();

        // Then
        assertThat(total).isEqualTo(10);
    }

    @Test
    void getTotalItems_shouldReturnZeroForEmptyCart() {
        // When
        int total = cartService.getTotalItems();

        // Then
        assertThat(total).isZero();
    }

    @Test
    void addToCart_shouldThrowExceptionForNonExistentProduct() {
        // When & Then
        assertThatThrownBy(() -> cartService.addToCart(999L, 1))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void cartOperations_shouldPersistAcrossMultipleOperations() {
        // Given - Multiple operations
        cartService.addToCart(product1.getId(), 1);
        cartService.addToCart(product2.getId(), 2);

        // When - More operations
        cartService.addToCart(product1.getId(), 1); // Increment
        cartService.updateQuantity(product2.getId(), 5); // Update

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items).hasSize(2);

        CartItem laptopItem = items.stream()
                .filter(item -> item.getProductId().equals(product1.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(laptopItem.getQuantity()).isEqualTo(2);

        CartItem mouseItem = items.stream()
                .filter(item -> item.getProductId().equals(product2.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(mouseItem.getQuantity()).isEqualTo(5);
    }

    @Test
    void calculateTotal_shouldHandleLargeQuantities() {
        // Given
        cartService.addToCart(product2.getId(), 100);

        // When
        BigDecimal total = cartService.calculateTotal();

        // Then
        BigDecimal expected = new BigDecimal("29.99").multiply(BigDecimal.valueOf(100));
        assertThat(total).isEqualByComparingTo(expected);
    }

    @Test
    void removeFromCart_shouldHandleNonExistentItem() {
        // Given
        cartService.addToCart(product1.getId(), 1);

        // When - Try to remove non-existent item
        cartService.removeFromCart(999L);

        // Then - Cart should remain unchanged
        assertThat(cartService.getCartItems()).hasSize(1);
    }

    @Test
    void updateQuantity_shouldHandleNonExistentItem() {
        // Given
        cartService.addToCart(product1.getId(), 1);

        // When - Try to update non-existent item
        cartService.updateQuantity(999L, 5);

        // Then - Cart should remain unchanged
        assertThat(cartService.getCartItems()).hasSize(1);
    }

    @Test
    void complexCartScenario_shouldWorkCorrectly() {
        TestDelayUtil.massiveDelay();
        // Scenario: User adds items, changes mind, updates quantities

        // Add initial items
        cartService.addToCart(product1.getId(), 1);
        cartService.addToCart(product2.getId(), 2);
        cartService.addToCart(product3.getId(), 3);
        assertThat(cartService.getTotalItems()).isEqualTo(6);

        // Change quantity of laptop
        cartService.updateQuantity(product1.getId(), 2);
        assertThat(cartService.getTotalItems()).isEqualTo(7);

        // Remove mouse
        cartService.removeFromCart(product2.getId());
        assertThat(cartService.getCartItems()).hasSize(2);
        assertThat(cartService.getTotalItems()).isEqualTo(5);

        // Add mouse back with different quantity
        cartService.addToCart(product2.getId(), 5);
        assertThat(cartService.getTotalItems()).isEqualTo(10);

        // Verify final total
        BigDecimal expectedTotal = new BigDecimal("1299.99").multiply(BigDecimal.valueOf(2))
                .add(new BigDecimal("29.99").multiply(BigDecimal.valueOf(5)))
                .add(new BigDecimal("89.99").multiply(BigDecimal.valueOf(3)));
        assertThat(cartService.calculateTotal()).isEqualByComparingTo(expectedTotal);
    }
}
