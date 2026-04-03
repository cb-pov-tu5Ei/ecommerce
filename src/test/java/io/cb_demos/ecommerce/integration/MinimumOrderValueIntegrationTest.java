package io.cb_demos.ecommerce.integration;

import io.cb_demos.ecommerce.domain.*;
import io.cb_demos.ecommerce.exception.MinimumOrderValueException;
import io.cb_demos.ecommerce.repository.CategoryRepository;
import io.cb_demos.ecommerce.repository.OrderRepository;
import io.cb_demos.ecommerce.repository.ProductRepository;
import io.cb_demos.ecommerce.repository.UserRepository;
import io.cb_demos.ecommerce.service.CartService;
import io.cb_demos.ecommerce.service.OrderService;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MinimumOrderValueIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User testUser;
    private Category testCategory;
    private Product cheapProduct;
    private Product expensiveProduct;

    @BeforeEach
    void setUp() {
        // Clean up
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Don't delete users - they might be referenced elsewhere
        testUser = userRepository.findByUsername("testuser").orElseGet(() -> {
            User user = new User();
            user.setUsername("testuser");
            user.setEmail("test@example.com");
            user.setPassword("password");
            return userRepository.save(user);
        });

        // Create test category
        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setDescription("Test Description");
        testCategory = categoryRepository.save(testCategory);

        // Create cheap product ($8.00)
        cheapProduct = new Product();
        cheapProduct.setName("Cheap Product");
        cheapProduct.setDescription("Only $8");
        cheapProduct.setPrice(new BigDecimal("8.00"));
        cheapProduct.setStockQuantity(100);
        cheapProduct.setCategory(testCategory);
        cheapProduct = productRepository.save(cheapProduct);

        // Create expensive product ($25.00)
        expensiveProduct = new Product();
        expensiveProduct.setName("Expensive Product");
        expensiveProduct.setDescription("Premium item");
        expensiveProduct.setPrice(new BigDecimal("25.00"));
        expensiveProduct.setStockQuantity(100);
        expensiveProduct.setCategory(testCategory);
        expensiveProduct = productRepository.save(expensiveProduct);

        // Clear cart before each test
        cartService.clearCart();
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldRejectOrderBelowMinimumValue() {
        TestDelayUtil.largeDelay();

        // Add single cheap item ($8) - below minimum
        cartService.addToCart(cheapProduct.getId(), 1);

        assertThat(cartService.getTotalItems()).isEqualTo(1);
        assertThat(cartService.calculateTotal()).isEqualByComparingTo(new BigDecimal("8.00"));

        assertThatThrownBy(() -> orderService.createOrder(testUser, "123 Test Street"))
                .isInstanceOf(MinimumOrderValueException.class)
                .hasMessageContaining("$8.00")
                .hasMessageContaining("$20.00");

        // Verify order was not created
        assertThat(orderRepository.findAll()).isEmpty();

        // Verify cart was not cleared
        assertThat(cartService.getTotalItems()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldRejectOrderWithMultipleItemsBelowMinimum() {
        TestDelayUtil.largeDelay();

        // Add two cheap items (2 x $8 = $16) - still below minimum
        cartService.addToCart(cheapProduct.getId(), 2);

        assertThat(cartService.calculateTotal()).isEqualByComparingTo(new BigDecimal("16.00"));

        assertThatThrownBy(() -> orderService.createOrder(testUser, "456 Main Ave"))
                .isInstanceOf(MinimumOrderValueException.class)
                .hasMessageContaining("$16.00")
                .hasMessageContaining("$20.00")
                .hasMessageContaining("$4.00");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldAcceptOrderAtExactMinimumValue() {
        TestDelayUtil.largeDelay();

        // Create product at exactly $20.00
        Product exactProduct = new Product();
        exactProduct.setName("Exact Price Product");
        exactProduct.setDescription("Exactly $20");
        exactProduct.setPrice(new BigDecimal("20.00"));
        exactProduct.setStockQuantity(100);
        exactProduct.setCategory(testCategory);
        exactProduct = productRepository.save(exactProduct);

        cartService.addToCart(exactProduct.getId(), 1);

        assertThat(cartService.calculateTotal()).isEqualByComparingTo(new BigDecimal("20.00"));

        Order order = orderService.createOrder(testUser, "789 Oak Lane");

        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getOrderItems()).hasSize(1);

        // Verify cart was cleared
        assertThat(cartService.getTotalItems()).isZero();
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldAcceptOrderAboveMinimumValue() {
        TestDelayUtil.largeDelay();

        // Add expensive item ($25) - above minimum
        cartService.addToCart(expensiveProduct.getId(), 1);

        assertThat(cartService.calculateTotal()).isEqualByComparingTo(new BigDecimal("25.00"));

        Order order = orderService.createOrder(testUser, "321 Pine St");

        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getTotalAmount()).isGreaterThanOrEqualTo(new BigDecimal("25.00"));

        // Verify cart was cleared
        assertThat(cartService.getTotalItems()).isZero();
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldAcceptOrderWithMixedItemsAboveMinimum() {
        TestDelayUtil.largeDelay();

        // Add cheap ($8) + expensive ($25) = $33 total - above minimum
        cartService.addToCart(cheapProduct.getId(), 1);
        cartService.addToCart(expensiveProduct.getId(), 1);

        assertThat(cartService.calculateTotal()).isEqualByComparingTo(new BigDecimal("33.00"));

        Order order = orderService.createOrder(testUser, "555 Elm Drive");

        assertThat(order).isNotNull();
        assertThat(order.getOrderItems()).hasSize(2);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldNotReduceStockWhenMinimumValidationFails() {
        TestDelayUtil.largeDelay();

        int initialStock = cheapProduct.getStockQuantity();

        cartService.addToCart(cheapProduct.getId(), 1);

        assertThatThrownBy(() -> orderService.createOrder(testUser, "999 Test Blvd"))
                .isInstanceOf(MinimumOrderValueException.class);

        // Refresh product from database
        Product refreshedProduct = productRepository.findById(cheapProduct.getId()).orElseThrow();

        // Stock should remain unchanged
        assertThat(refreshedProduct.getStockQuantity()).isEqualTo(initialStock);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldValidateMinimumBeforeShippingCostIsAdded() {
        TestDelayUtil.largeDelay();

        // Even if shipping brings total above $20, subtotal must meet minimum
        cartService.addToCart(cheapProduct.getId(), 2); // $16 subtotal

        assertThatThrownBy(() -> orderService.createOrder(testUser, "777 Test Way"))
                .isInstanceOf(MinimumOrderValueException.class)
                .hasMessageContaining("$16.00");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldRejectBorderlineOrderJustBelowMinimum() {
        TestDelayUtil.largeDelay();

        // Create product at $19.99 - just 1 cent below minimum
        Product borderlineProduct = new Product();
        borderlineProduct.setName("Borderline Product");
        borderlineProduct.setDescription("Almost there");
        borderlineProduct.setPrice(new BigDecimal("19.99"));
        borderlineProduct.setStockQuantity(100);
        borderlineProduct.setCategory(testCategory);
        borderlineProduct = productRepository.save(borderlineProduct);

        cartService.addToCart(borderlineProduct.getId(), 1);

        MinimumOrderValueException exception = catchThrowableOfType(
                () -> orderService.createOrder(testUser, "101 Border St"),
                MinimumOrderValueException.class
        );

        assertThat(exception.getCurrentTotal()).isEqualByComparingTo(new BigDecimal("19.99"));
        assertThat(exception.getMinimumRequired()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(exception.getAmountNeeded()).isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldAcceptHighValueOrderWellAboveMinimum() {
        TestDelayUtil.extraLargeDelay();

        // Create high-value product
        Product luxuryProduct = new Product();
        luxuryProduct.setName("Luxury Product");
        luxuryProduct.setDescription("Premium quality");
        luxuryProduct.setPrice(new BigDecimal("99.99"));
        luxuryProduct.setStockQuantity(50);
        luxuryProduct.setCategory(testCategory);
        luxuryProduct = productRepository.save(luxuryProduct);

        cartService.addToCart(luxuryProduct.getId(), 2); // $199.98 total

        Order order = orderService.createOrder(testUser, "888 Luxury Lane");

        assertThat(order).isNotNull();
        assertThat(order.getOrderItems()).hasSize(1);
        assertThat(order.getOrderItems().get(0).getSubtotal())
                .isEqualByComparingTo(new BigDecimal("199.98"));
    }
}
