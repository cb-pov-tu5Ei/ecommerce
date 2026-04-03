package io.cb_demos.ecommerce.integration;

import io.cb_demos.ecommerce.domain.Order;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.domain.User;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CheckoutFlowIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("integrationtest");
        testUser.setEmail("integration@test.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setFirstName("Integration");
        testUser.setLastName("Test");
        testUser.setRole(User.UserRole.USER);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Create test product
        testProduct = new Product();
        testProduct.setName("Integration Test Product");
        testProduct.setDescription("Product for integration testing");
        testProduct.setPrice(new BigDecimal("49.99"));
        testProduct.setStockQuantity(10);
        testProduct.setActive(true);
        testProduct = productRepository.save(testProduct);

        // Clear cart
        cartService.clearCart();
    }

    @Test
    void completeCheckoutFlow_shouldCreateOrderAndReduceStock() {
        TestDelayUtil.extraLargeDelay();
        // Given - Initial state
        int initialStock = testProduct.getStockQuantity();
        int orderQuantity = 2;

        // When - Add to cart
        cartService.addToCart(testProduct.getId(), orderQuantity);

        // Then - Verify cart
        assertThat(cartService.getTotalItems()).isEqualTo(orderQuantity);
        assertThat(cartService.calculateTotal()).isEqualByComparingTo(
                testProduct.getPrice().multiply(BigDecimal.valueOf(orderQuantity))
        );

        // When - Create order
        Order order = orderService.createOrder(testUser, "123 Test Street, Test City");

        // Then - Verify order created
        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getOrderNumber()).isNotNull();
        assertThat(order.getUser()).isEqualTo(testUser);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("99.98"));
        assertThat(order.getOrderItems()).hasSize(1);
        assertThat(order.getOrderItems().get(0).getQuantity()).isEqualTo(orderQuantity);

        // Then - Verify cart cleared
        assertThat(cartService.getTotalItems()).isZero();

        // Then - Verify stock reduced
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(initialStock - orderQuantity);

        // Then - Verify order persisted
        Order savedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(savedOrder.getOrderNumber()).isEqualTo(order.getOrderNumber());
    }

    @Test
    void multipleProductCheckout_shouldCalculateTotalCorrectly() {
        TestDelayUtil.massiveDelay();
        // Given - Create second product
        Product secondProduct = new Product();
        secondProduct.setName("Second Product");
        secondProduct.setDescription("Another test product");
        secondProduct.setPrice(new BigDecimal("25.50"));
        secondProduct.setStockQuantity(20);
        secondProduct.setActive(true);
        secondProduct = productRepository.save(secondProduct);

        // When - Add multiple products to cart
        cartService.addToCart(testProduct.getId(), 3);
        cartService.addToCart(secondProduct.getId(), 2);

        // Then - Verify cart totals
        assertThat(cartService.getTotalItems()).isEqualTo(5);
        BigDecimal expectedTotal = testProduct.getPrice().multiply(BigDecimal.valueOf(3))
                .add(secondProduct.getPrice().multiply(BigDecimal.valueOf(2)));
        assertThat(cartService.calculateTotal()).isEqualByComparingTo(expectedTotal);

        // When - Create order
        Order order = orderService.createOrder(testUser, "456 Test Ave");

        // Then - Verify order
        assertThat(order.getOrderItems()).hasSize(2);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    void findUserOrders_shouldReturnOrderHistory() {
        TestDelayUtil.largeDelay();
        // Given - Create multiple orders
        cartService.addToCart(testProduct.getId(), 1);
        Order order1 = orderService.createOrder(testUser, "Order 1 Address");

        cartService.addToCart(testProduct.getId(), 2);
        Order order2 = orderService.createOrder(testUser, "Order 2 Address");

        // When - Find user's orders
        List<Order> orders = orderRepository.findByUserOrderByOrderDateDesc(
                testUser, org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        // Then - Verify order history
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getId()).isEqualTo(order2.getId()); // Most recent first
        assertThat(orders.get(1).getId()).isEqualTo(order1.getId());
    }
}
