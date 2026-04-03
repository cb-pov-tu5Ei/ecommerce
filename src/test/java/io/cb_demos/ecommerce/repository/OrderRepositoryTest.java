package io.cb_demos.ecommerce.repository;

import io.cb_demos.ecommerce.domain.Order;
import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(User.UserRole.USER);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setRole(User.UserRole.USER);
        otherUser.setEnabled(true);
        otherUser = userRepository.save(otherUser);
    }

    @Test
    void findByUserOrderByOrderDateDesc_shouldReturnUserOrdersInDescendingOrder() {
        TestDelayUtil.mediumDelay();
        // Given
        Order order1 = createOrder(testUser, "ORD-001", LocalDateTime.now().minusDays(3));
        Order order2 = createOrder(testUser, "ORD-002", LocalDateTime.now().minusDays(1));
        Order order3 = createOrder(testUser, "ORD-003", LocalDateTime.now());
        createOrder(otherUser, "ORD-004", LocalDateTime.now());

        // When
        Page<Order> results = orderRepository.findByUserOrderByOrderDateDesc(
                testUser, PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(3);
        assertThat(results.getContent().get(0)).isEqualTo(order3); // Most recent
        assertThat(results.getContent().get(1)).isEqualTo(order2);
        assertThat(results.getContent().get(2)).isEqualTo(order1); // Oldest
    }

    @Test
    void findByUserOrderByOrderDateDesc_shouldOnlyReturnUserOrders() {
        // Given
        Order testUserOrder1 = createOrder(testUser, "ORD-001", LocalDateTime.now());
        Order testUserOrder2 = createOrder(testUser, "ORD-002", LocalDateTime.now());
        createOrder(otherUser, "ORD-003", LocalDateTime.now());
        createOrder(otherUser, "ORD-004", LocalDateTime.now());

        // When
        Page<Order> results = orderRepository.findByUserOrderByOrderDateDesc(
                testUser, PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent()).contains(testUserOrder1, testUserOrder2);
    }

    @Test
    void findByUserOrderByOrderDateDesc_shouldSupportPagination() {
        TestDelayUtil.mediumDelay();
        // Given - Create 15 orders for test user
        for (int i = 1; i <= 15; i++) {
            createOrder(testUser, "ORD-" + String.format("%03d", i),
                    LocalDateTime.now().minusDays(15 - i));
        }

        // When
        Page<Order> page1 = orderRepository.findByUserOrderByOrderDateDesc(
                testUser, PageRequest.of(0, 5));
        Page<Order> page2 = orderRepository.findByUserOrderByOrderDateDesc(
                testUser, PageRequest.of(1, 5));
        Page<Order> page3 = orderRepository.findByUserOrderByOrderDateDesc(
                testUser, PageRequest.of(2, 5));

        // Then
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page2.getContent()).hasSize(5);
        assertThat(page3.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isEqualTo(15);
        assertThat(page1.getTotalPages()).isEqualTo(3);
    }

    @Test
    void findByUserOrderByOrderDateDesc_shouldReturnEmptyForUserWithNoOrders() {
        // Given
        createOrder(testUser, "ORD-001", LocalDateTime.now());

        // When
        Page<Order> results = orderRepository.findByUserOrderByOrderDateDesc(
                otherUser, PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).isEmpty();
    }

    @Test
    void findByOrderNumber_shouldReturnOrderWhenExists() {
        // Given
        Order order = createOrder(testUser, "ORD-123456", LocalDateTime.now());

        // When
        var result = orderRepository.findByOrderNumber("ORD-123456");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(order);
    }

    @Test
    void findByOrderNumber_shouldReturnEmptyWhenNotExists() {
        // When
        var result = orderRepository.findByOrderNumber("NONEXISTENT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void save_shouldPersistOrder() {
        // Given
        Order order = new Order();
        order.setOrderNumber("NEW-ORDER-001");
        order.setUser(testUser);
        order.setTotalAmount(new BigDecimal("99.99"));
        order.setStatus(Order.OrderStatus.PENDING);
        order.setShippingAddress("123 Test St");
        order.setOrderDate(LocalDateTime.now());

        // When
        Order saved = orderRepository.save(order);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(orderRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void delete_shouldRemoveOrder() {
        // Given
        Order order = createOrder(testUser, "ORD-DELETE", LocalDateTime.now());
        Long orderId = order.getId();

        // When
        orderRepository.delete(order);

        // Then
        assertThat(orderRepository.findById(orderId)).isEmpty();
    }

    @Test
    void findByStatus_shouldReturnOrdersWithGivenStatus() {
        // Given
        Order pending1 = createOrder(testUser, "ORD-P1", LocalDateTime.now());
        pending1.setStatus(Order.OrderStatus.PENDING);
        orderRepository.save(pending1);

        Order pending2 = createOrder(testUser, "ORD-P2", LocalDateTime.now());
        pending2.setStatus(Order.OrderStatus.PENDING);
        orderRepository.save(pending2);

        Order shipped = createOrder(testUser, "ORD-S1", LocalDateTime.now());
        shipped.setStatus(Order.OrderStatus.SHIPPED);
        orderRepository.save(shipped);

        // When
        Page<Order> pendingOrders = orderRepository.findByStatus(
                Order.OrderStatus.PENDING, PageRequest.of(0, 10));

        // Then
        assertThat(pendingOrders.getContent()).hasSize(2);
        assertThat(pendingOrders.getContent()).allMatch(
                o -> o.getStatus() == Order.OrderStatus.PENDING);
    }

    @Test
    void findByUserAndStatus_shouldFilterByUserAndStatus() {
        // Given
        Order testUserPending = createOrder(testUser, "ORD-TP", LocalDateTime.now());
        testUserPending.setStatus(Order.OrderStatus.PENDING);
        orderRepository.save(testUserPending);

        Order testUserShipped = createOrder(testUser, "ORD-TS", LocalDateTime.now());
        testUserShipped.setStatus(Order.OrderStatus.SHIPPED);
        orderRepository.save(testUserShipped);

        Order otherUserPending = createOrder(otherUser, "ORD-OP", LocalDateTime.now());
        otherUserPending.setStatus(Order.OrderStatus.PENDING);
        orderRepository.save(otherUserPending);

        // When
        Page<Order> results = orderRepository.findByUserAndStatus(
                testUser, Order.OrderStatus.PENDING, PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0)).isEqualTo(testUserPending);
    }

    @Test
    void countByUser_shouldReturnCorrectCount() {
        // Given
        createOrder(testUser, "ORD-1", LocalDateTime.now());
        createOrder(testUser, "ORD-2", LocalDateTime.now());
        createOrder(testUser, "ORD-3", LocalDateTime.now());
        createOrder(otherUser, "ORD-4", LocalDateTime.now());

        // When
        long count = orderRepository.countByUser(testUser);

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void findAll_shouldReturnAllOrders() {
        // Given
        createOrder(testUser, "ORD-1", LocalDateTime.now());
        createOrder(testUser, "ORD-2", LocalDateTime.now());
        createOrder(otherUser, "ORD-3", LocalDateTime.now());

        // When
        var allOrders = orderRepository.findAll();

        // Then
        assertThat(allOrders).hasSize(3);
    }

    @Test
    void findByUserAndStatus_shouldReturnUserOrdersWithStatus() {
        // Given
        createOrder(testUser, "ORD-1", LocalDateTime.now()).setStatus(Order.OrderStatus.PENDING);
        Order confirmed = createOrder(testUser, "ORD-2", LocalDateTime.now());
        confirmed.setStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(confirmed);
        Order cancelled = createOrder(testUser, "ORD-3", LocalDateTime.now());
        cancelled.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(cancelled);
        createOrder(otherUser, "ORD-4", LocalDateTime.now()).setStatus(Order.OrderStatus.PENDING);

        // When
        var pendingOrders = orderRepository.findByUserAndStatus(testUser, Order.OrderStatus.PENDING);
        var confirmedOrders = orderRepository.findByUserAndStatus(testUser, Order.OrderStatus.CONFIRMED);

        // Then
        assertThat(pendingOrders).hasSize(1);
        assertThat(confirmedOrders).hasSize(1);
        assertThat(confirmedOrders.get(0).getOrderNumber()).isEqualTo("ORD-2");
    }

    @Test
    void save_shouldPersistCancellationFields() {
        // Given
        Order order = createOrder(testUser, "ORD-CANCEL", LocalDateTime.now());
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason("Customer request");

        // When
        Order saved = orderRepository.save(order);
        Order reloaded = orderRepository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(reloaded.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        assertThat(reloaded.getCancelledAt()).isNotNull();
        assertThat(reloaded.getCancellationReason()).isEqualTo("Customer request");
    }

    private Order createOrder(User user, String orderNumber, LocalDateTime orderDate) {
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setStatus(Order.OrderStatus.PENDING);
        order.setShippingAddress("123 Test Street");
        order.setOrderDate(orderDate);
        return orderRepository.save(order);
    }
}
