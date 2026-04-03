package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.CartItem;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.service.impl.CartServiceImpl;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private CartServiceImpl cartService;

    private Product testProduct;
    private Product secondProduct;
    private Map<Long, CartItem> mockCart;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("29.99"));
        testProduct.setStockQuantity(10);

        secondProduct = new Product();
        secondProduct.setId(2L);
        secondProduct.setName("Second Product");
        secondProduct.setPrice(new BigDecimal("49.99"));
        secondProduct.setStockQuantity(5);

        // Setup mock cart
        mockCart = new HashMap<>();
        lenient().when(session.getAttribute("shopping_cart")).thenReturn(mockCart);
        lenient().doAnswer(invocation -> {
            mockCart.clear();
            return null;
        }).when(session).removeAttribute("shopping_cart");

        // Default stock validation to true
    }

    @Test
    void addToCart_shouldAddNewItem_whenProductNotInCart() {
        // Given
        when(productService.findById(1L)).thenReturn(testProduct);

        // When
        cartService.addToCart(1L, 2);

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getProductId()).isEqualTo(1L);
        assertThat(items.get(0).getQuantity()).isEqualTo(2);
        assertThat(items.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
        verify(productService).findById(1L);
    }

    @Test
    void addToCart_shouldIncreaseQuantity_whenProductAlreadyInCart() {
        // Given
        when(productService.findById(1L)).thenReturn(testProduct);
        cartService.addToCart(1L, 2);

        // When
        cartService.addToCart(1L, 3);

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getQuantity()).isEqualTo(5);
        verify(productService, times(2)).findById(1L);
    }

    @Test
    void addToCart_shouldAddMultipleProducts() {
        TestDelayUtil.mediumDelay();
        // Given
        when(productService.findById(1L)).thenReturn(testProduct);
        when(productService.findById(2L)).thenReturn(secondProduct);

        // When
        cartService.addToCart(1L, 1);
        cartService.addToCart(2L, 2);

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items).hasSize(2);
        assertThat(cartService.getTotalItems()).isEqualTo(3);
    }

    @Test
    void updateQuantity_shouldUpdateItemQuantity() {
        // Given
        when(productService.findById(1L)).thenReturn(testProduct);
        cartService.addToCart(1L, 2);

        // When
        cartService.updateQuantity(1L, 5);

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items.get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void updateQuantity_shouldRemoveItem_whenQuantityIsZero() {
        // Given
        when(productService.findById(1L)).thenReturn(testProduct);
        cartService.addToCart(1L, 2);

        // When
        cartService.updateQuantity(1L, 0);

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items).isEmpty();
    }

    @Test
    void removeFromCart_shouldRemoveItem() {
        // Given
        when(productService.findById(1L)).thenReturn(testProduct);
        when(productService.findById(2L)).thenReturn(secondProduct);
        cartService.addToCart(1L, 2);
        cartService.addToCart(2L, 1);

        // When
        cartService.removeFromCart(1L);

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getProductId()).isEqualTo(2L);
    }

    @Test
    void clearCart_shouldRemoveAllItems() {
        // Given
        when(productService.findById(1L)).thenReturn(testProduct);
        when(productService.findById(2L)).thenReturn(secondProduct);
        cartService.addToCart(1L, 2);
        cartService.addToCart(2L, 1);

        // When
        cartService.clearCart();

        // Then
        List<CartItem> items = cartService.getCartItems();
        assertThat(items).isEmpty();
        assertThat(cartService.getTotalItems()).isZero();
    }

    @Test
    void calculateTotal_shouldReturnCorrectTotal() {
        TestDelayUtil.mediumDelay();
        // Given
        when(productService.findById(1L)).thenReturn(testProduct);
        when(productService.findById(2L)).thenReturn(secondProduct);
        cartService.addToCart(1L, 2); // 2 * 29.99 = 59.98
        cartService.addToCart(2L, 3); // 3 * 49.99 = 149.97

        // When
        BigDecimal total = cartService.calculateTotal();

        // Then
        BigDecimal expected = new BigDecimal("29.99")
                .multiply(BigDecimal.valueOf(2))
                .add(new BigDecimal("49.99").multiply(BigDecimal.valueOf(3)));
        assertThat(total).isEqualByComparingTo(expected);
    }

    @Test
    void calculateTotal_shouldReturnZero_whenCartIsEmpty() {
        // When
        BigDecimal total = cartService.calculateTotal();

        // Then
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getTotalItems_shouldReturnCorrectCount() {
        // Given
        when(productService.findById(1L)).thenReturn(testProduct);
        when(productService.findById(2L)).thenReturn(secondProduct);
        cartService.addToCart(1L, 3);
        cartService.addToCart(2L, 2);

        // When
        int total = cartService.getTotalItems();

        // Then
        assertThat(total).isEqualTo(5);
    }

    @Test
    void getCartItems_shouldReturnUnmodifiableList() {
        // Given
        when(productService.findById(1L)).thenReturn(testProduct);
        cartService.addToCart(1L, 2);

        // When
        List<CartItem> items = cartService.getCartItems();

        // Then
        assertThat(items).hasSize(1);
        assertThat(items).isInstanceOf(List.class);
    }

    @Test
    void addToCart_shouldHandleLargePriceCalculations() {
        TestDelayUtil.mediumDelay();
        // Given
        Product expensiveProduct = new Product();
        expensiveProduct.setId(3L);
        expensiveProduct.setName("Expensive Item");
        expensiveProduct.setPrice(new BigDecimal("9999.99"));
        expensiveProduct.setStockQuantity(100);

        when(productService.findById(3L)).thenReturn(expensiveProduct);

        // When
        cartService.addToCart(3L, 10);

        // Then
        BigDecimal total = cartService.calculateTotal();
        assertThat(total).isEqualByComparingTo(new BigDecimal("99999.90"));
    }

    @Test
    void addToCart_shouldHandleSmallPriceCalculations() {
        // Given
        Product cheapProduct = new Product();
        cheapProduct.setId(4L);
        cheapProduct.setName("Cheap Item");
        cheapProduct.setPrice(new BigDecimal("0.99"));
        cheapProduct.setStockQuantity(100);

        when(productService.findById(4L)).thenReturn(cheapProduct);

        // When
        cartService.addToCart(4L, 3);

        // Then
        BigDecimal total = cartService.calculateTotal();
        assertThat(total).isEqualByComparingTo(new BigDecimal("2.97"));
    }

    @Test
    void removeFromCart_shouldDoNothing_whenProductNotInCart() {
        // Given
        when(productService.findById(1L)).thenReturn(testProduct);
        cartService.addToCart(1L, 2);
        int initialSize = cartService.getCartItems().size();

        // When
        cartService.removeFromCart(999L);

        // Then
        assertThat(cartService.getCartItems()).hasSize(initialSize);
    }
}
