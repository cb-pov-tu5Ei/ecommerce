package io.cb_demos.ecommerce.integration;

import io.cb_demos.ecommerce.domain.Order;
import io.cb_demos.ecommerce.domain.OrderItem;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.exception.InsufficientStockException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class OrderManagementIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("ordertest");
        testUser.setEmail("order@test.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setFirstName("Order");
        testUser.setLastName("Test");
        testUser.setRole(User.UserRole.USER);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Create test products
        product1 = new Product();
        product1.setName("Product 1");
        product1.setDescription("First test product");
        product1.setPrice(new BigDecimal("99.99"));
        product1.setStockQuantity(10);
        product1.setActive(true);
        product1 = productRepository.save(product1);

        product2 = new Product();
        product2.setName("Product 2");
        product2.setDescription("Second test product");
        product2.setPrice(new BigDecimal("49.99"));
        product2.setStockQuantity(5);
        product2.setActive(true);
        product2 = productRepository.save(product2);

        product3 = new Product();
        product3.setName("Product 3");
        product3.setDescription("Third test product");
        product3.setPrice(new BigDecimal("29.99"));
        product3.setStockQuantity(3);
        product3.setActive(true);
        product3 = productRepository.save(product3);

        // Clear cart
        cartService.clearCart();
    }

    @Test
    void createOrder_shouldGenerateUniqueOrderNumber() {
        TestDelayUtil.largeDelay();
        // Given
        cartService.addToCart(product1.getId(), 1);

        // When
        Order order1 = orderService.createOrder(testUser, "Address 1");

        cartService.addToCart(product1.getId(), 1);
        Order order2 = orderService.createOrder(testUser, "Address 2");

        // Then
        assertThat(order1.getOrderNumber()).isNotNull();
        assertThat(order2.getOrderNumber()).isNotNull();
        assertThat(order1.getOrderNumber()).isNotEqualTo(order2.getOrderNumber());
    }

    @Test
    void createOrder_shouldCalculateTotalCorrectly() {
        TestDelayUtil.extraLargeDelay();
        // Given
        cartService.addToCart(product1.getId(), 2); // 2 * 99.99 = 199.98
        cartService.addToCart(product2.getId(), 1); // 1 * 49.99 = 49.99
        cartService.addToCart(product3.getId(), 3); // 3 * 29.99 = 89.97

        // When
        Order order = orderService.createOrder(testUser, "123 Test Street");

        // Then
        BigDecimal expectedTotal = new BigDecimal("99.99")
                .multiply(BigDecimal.valueOf(2))
                .add(new BigDecimal("49.99"))
                .add(new BigDecimal("29.99").multiply(BigDecimal.valueOf(3)));

        assertThat(order.getTotalAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(order.getOrderItems()).hasSize(3);
    }

    @Test
    void createOrder_shouldReduceStockQuantity() {
        TestDelayUtil.massiveDelay();
        // Given
        int initialStock1 = product1.getStockQuantity();
        int initialStock2 = product2.getStockQuantity();

        cartService.addToCart(product1.getId(), 2);
        cartService.addToCart(product2.getId(), 1);

        // When
        orderService.createOrder(testUser, "123 Test Street");

        // Then
        Product updatedProduct1 = productRepository.findById(product1.getId()).orElseThrow();
        Product updatedProduct2 = productRepository.findById(product2.getId()).orElseThrow();

        assertThat(updatedProduct1.getStockQuantity()).isEqualTo(initialStock1 - 2);
        assertThat(updatedProduct2.getStockQuantity()).isEqualTo(initialStock2 - 1);
    }

    @Test
    void createOrder_shouldFailWithInsufficientStock() {
        TestDelayUtil.mediumDelay();
        // Given
        cartService.addToCart(product1.getId(), 20); // Only 10 in stock

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(testUser, "123 Test Street"))
                .isInstanceOf(InsufficientStockException.class);

        // Verify order was not created
        assertThat(orderRepository.count()).isZero();

        // Verify stock was not reduced
        Product unchangedProduct = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(unchangedProduct.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void createOrder_shouldClearCartAfterSuccess() {
        TestDelayUtil.mediumDelay();
        // Given
        cartService.addToCart(product1.getId(), 1);
        assertThat(cartService.getTotalItems()).isEqualTo(1);

        // When
        orderService.createOrder(testUser, "123 Test Street");

        // Then
        assertThat(cartService.getTotalItems()).isZero();
        assertThat(cartService.getCartItems()).isEmpty();
    }

    @Test
    void createOrder_shouldNotClearCartOnFailure() {
        // Given
        cartService.addToCart(product1.getId(), 20); // Insufficient stock
        assertThat(cartService.getTotalItems()).isEqualTo(20);

        // When
        try {
            orderService.createOrder(testUser, "123 Test Street");
        } catch (InsufficientStockException e) {
            // Expected
        }

        // Then - Cart should still have items
        assertThat(cartService.getTotalItems()).isEqualTo(20);
    }

    @Test
    void createOrder_shouldSetInitialStatusToPending() {
        // Given
        cartService.addToCart(product1.getId(), 1);

        // When
        Order order = orderService.createOrder(testUser, "123 Test Street");

        // Then
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
    }

    @Test
    void updateOrderStatus_shouldChangeStatus() {
        TestDelayUtil.mediumDelay();
        // Given
        cartService.addToCart(product1.getId(), 1);
        Order order = orderService.createOrder(testUser, "123 Test Street");
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);

        // When
        Order shipped = orderService.updateOrderStatus(order.getId(), Order.OrderStatus.SHIPPED);

        // Then
        assertThat(shipped.getStatus()).isEqualTo(Order.OrderStatus.SHIPPED);

        // Verify persistence
        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Order.OrderStatus.SHIPPED);
    }

    @Test
    void updateOrderStatus_shouldSupportAllStatuses() {
        TestDelayUtil.mediumDelay();
        // Given
        cartService.addToCart(product1.getId(), 1);
        Order order = orderService.createOrder(testUser, "123 Test Street");

        // When & Then - Test all status transitions
        order = orderService.updateOrderStatus(order.getId(), Order.OrderStatus.CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);

        order = orderService.updateOrderStatus(order.getId(), Order.OrderStatus.PROCESSING);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PROCESSING);

        order = orderService.updateOrderStatus(order.getId(), Order.OrderStatus.SHIPPED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.SHIPPED);

        order = orderService.updateOrderStatus(order.getId(), Order.OrderStatus.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);
    }

    @Test
    void findUserOrders_shouldReturnOnlyUserOrders() {
        TestDelayUtil.extraLargeDelay();
        // Given - Create orders for test user
        cartService.addToCart(product1.getId(), 1);
        Order order1 = orderService.createOrder(testUser, "Address 1");

        cartService.addToCart(product2.getId(), 1);
        Order order2 = orderService.createOrder(testUser, "Address 2");

        // Create another user and their order
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@test.com");
        otherUser.setPassword(passwordEncoder.encode("password"));
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setRole(User.UserRole.USER);
        otherUser.setEnabled(true);
        otherUser = userRepository.save(otherUser);

        cartService.addToCart(product3.getId(), 1);
        orderService.createOrder(otherUser, "Other Address");

        // When
        Page<Order> userOrders = orderService.findUserOrders(testUser, PageRequest.of(0, 10));

        // Then
        assertThat(userOrders.getContent()).hasSize(2);
        assertThat(userOrders.getContent()).extracting(Order::getId)
                .containsExactlyInAnyOrder(order1.getId(), order2.getId());
    }

    @Test
    void findUserOrders_shouldOrderByDateDescending() {
        TestDelayUtil.mediumDelay();
        // Given
        cartService.addToCart(product1.getId(), 1);
        Order order1 = orderService.createOrder(testUser, "Address 1");

        cartService.addToCart(product2.getId(), 1);
        Order order2 = orderService.createOrder(testUser, "Address 2");

        cartService.addToCart(product3.getId(), 1);
        Order order3 = orderService.createOrder(testUser, "Address 3");

        // When
        Page<Order> userOrders = orderService.findUserOrders(testUser, PageRequest.of(0, 10));

        // Then
        assertThat(userOrders.getContent()).hasSize(3);
        // Most recent should be first
        assertThat(userOrders.getContent().get(0).getId()).isEqualTo(order3.getId());
        assertThat(userOrders.getContent().get(1).getId()).isEqualTo(order2.getId());
        assertThat(userOrders.getContent().get(2).getId()).isEqualTo(order1.getId());
    }

    @Test
    void orderItems_shouldContainCorrectProductDetails() {
        TestDelayUtil.mediumDelay();
        // Given
        cartService.addToCart(product1.getId(), 2);
        cartService.addToCart(product2.getId(), 1);

        // When
        Order order = orderService.createOrder(testUser, "123 Test Street");

        // Then
        assertThat(order.getOrderItems()).hasSize(2);

        OrderItem item1 = order.getOrderItems().stream()
                .filter(i -> i.getProduct().getId().equals(product1.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(item1.getQuantity()).isEqualTo(2);
        assertThat(item1.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(item1.getProduct().getName()).isEqualTo("Product 1");
    }

    @Test
    void createMultipleOrders_shouldMaintainInventoryAccurately() {
        TestDelayUtil.mediumDelay();
        // Given
        int initialStock = product1.getStockQuantity();

        // When - Create multiple orders
        cartService.addToCart(product1.getId(), 2);
        orderService.createOrder(testUser, "Order 1");

        cartService.addToCart(product1.getId(), 3);
        orderService.createOrder(testUser, "Order 2");

        cartService.addToCart(product1.getId(), 1);
        orderService.createOrder(testUser, "Order 3");

        // Then
        Product finalProduct = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(finalProduct.getStockQuantity()).isEqualTo(initialStock - 6);
    }

    @Test
    void createOrder_shouldFailWithEmptyCart() {
        // Given - Empty cart
        cartService.clearCart();

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(testUser, "123 Test Street"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty cart");
    }

    @Test
    void createOrder_shouldStoreShippingAddress() {
        TestDelayUtil.mediumDelay();
        // Given
        cartService.addToCart(product1.getId(), 1);
        String address = "456 Shipping Lane, Test City, TC 12345";

        // When
        Order order = orderService.createOrder(testUser, address);

        // Then
        assertThat(order.getShippingAddress()).isEqualTo(address);

        // Verify persistence
        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getShippingAddress()).isEqualTo(address);
    }

    @Test
    void createOrder_shouldHandleLowStock() {
        TestDelayUtil.mediumDelay();
        // Given
        assertThat(product3.getStockQuantity()).isEqualTo(3);
        cartService.addToCart(product3.getId(), 3);

        // When
        Order order = orderService.createOrder(testUser, "123 Test Street");

        // Then
        assertThat(order).isNotNull();
        Product depleted = productRepository.findById(product3.getId()).orElseThrow();
        assertThat(depleted.getStockQuantity()).isZero();
    }

    @Test
    void cancelOrder_shouldRestoreStockCompletely() {
        TestDelayUtil.extraLargeDelay();
        // Given
        int initialStock1 = product1.getStockQuantity();
        int initialStock2 = product2.getStockQuantity();

        cartService.addToCart(product1.getId(), 2);
        cartService.addToCart(product2.getId(), 1);
        Order order = orderService.createOrder(testUser, "123 Test Street");

        // Verify stock was reduced
        Product afterOrder1 = productRepository.findById(product1.getId()).orElseThrow();
        Product afterOrder2 = productRepository.findById(product2.getId()).orElseThrow();
        assertThat(afterOrder1.getStockQuantity()).isEqualTo(initialStock1 - 2);
        assertThat(afterOrder2.getStockQuantity()).isEqualTo(initialStock2 - 1);

        // When
        Order cancelled = orderService.cancelOrder(order.getId(), testUser);

        // Then
        assertThat(cancelled.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);

        Product restored1 = productRepository.findById(product1.getId()).orElseThrow();
        Product restored2 = productRepository.findById(product2.getId()).orElseThrow();
        assertThat(restored1.getStockQuantity()).isEqualTo(initialStock1);
        assertThat(restored2.getStockQuantity()).isEqualTo(initialStock2);
    }

    @Test
    void cancelOrder_shouldHandleMultipleItemsCorrectly() {
        TestDelayUtil.largeDelay();
        // Given
        int initialStock1 = product1.getStockQuantity();
        int initialStock2 = product2.getStockQuantity();
        int initialStock3 = product3.getStockQuantity();

        cartService.addToCart(product1.getId(), 1);
        cartService.addToCart(product2.getId(), 2);
        cartService.addToCart(product3.getId(), 3);
        Order order = orderService.createOrder(testUser, "123 Test Street");

        // When
        Order cancelled = orderService.cancelOrder(order.getId(), testUser);

        // Then
        assertThat(cancelled.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);

        Product restored1 = productRepository.findById(product1.getId()).orElseThrow();
        Product restored2 = productRepository.findById(product2.getId()).orElseThrow();
        Product restored3 = productRepository.findById(product3.getId()).orElseThrow();

        assertThat(restored1.getStockQuantity()).isEqualTo(initialStock1);
        assertThat(restored2.getStockQuantity()).isEqualTo(initialStock2);
        assertThat(restored3.getStockQuantity()).isEqualTo(initialStock3);
    }

    @Test
    void cancelOrder_shouldMaintainInventoryAccuracyAcrossCancellations() {
        TestDelayUtil.largeDelay();
        // Given
        int initialStock = product1.getStockQuantity();

        // Create two orders
        cartService.addToCart(product1.getId(), 2);
        Order order1 = orderService.createOrder(testUser, "Address 1");

        cartService.addToCart(product1.getId(), 3);
        Order order2 = orderService.createOrder(testUser, "Address 2");

        // Stock should be reduced by 5 total
        Product afterOrders = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(afterOrders.getStockQuantity()).isEqualTo(initialStock - 5);

        // When - Cancel first order only
        orderService.cancelOrder(order1.getId(), testUser);

        // Then
        Product afterCancel = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(afterCancel.getStockQuantity()).isEqualTo(initialStock - 3);

        // Verify order2 is still active
        Order reloadedOrder2 = orderRepository.findById(order2.getId()).orElseThrow();
        assertThat(reloadedOrder2.getStatus()).isNotEqualTo(Order.OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_shouldBeAtomicOnFailure() {
        TestDelayUtil.extraLargeDelay();
        // Given
        cartService.addToCart(product1.getId(), 2);
        Order order = orderService.createOrder(testUser, "123 Test Street");

        // Delete a product to simulate failure during stock restoration
        productRepository.deleteById(product1.getId());

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(order.getId(), testUser))
                .isInstanceOf(Exception.class);

        // Verify order status was rolled back
        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
    }

    @Test
    void cancelOrder_shouldNotAffectOtherUserOrders() {
        TestDelayUtil.largeDelay();
        // Given
        // Create another user
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@test.com");
        otherUser.setPassword(passwordEncoder.encode("password"));
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setRole(User.UserRole.USER);
        otherUser.setEnabled(true);
        otherUser = userRepository.save(otherUser);

        // Create order for testUser
        cartService.addToCart(product1.getId(), 2);
        Order testUserOrder = orderService.createOrder(testUser, "Test Address");

        // Create order for otherUser
        cartService.addToCart(product2.getId(), 1);
        Order otherUserOrder = orderService.createOrder(otherUser, "Other Address");

        // When - testUser tries to cancel otherUser's order (should fail)
        assertThatThrownBy(() -> orderService.cancelOrder(otherUserOrder.getId(), testUser))
                .isInstanceOf(Exception.class);

        // Then - otherUser's order should be unchanged
        Order reloadedOtherOrder = orderRepository.findById(otherUserOrder.getId()).orElseThrow();
        assertThat(reloadedOtherOrder.getStatus()).isEqualTo(Order.OrderStatus.PENDING);

        // testUser can cancel their own order
        Order cancelled = orderService.cancelOrder(testUserOrder.getId(), testUser);
        assertThat(cancelled.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
    }
}
