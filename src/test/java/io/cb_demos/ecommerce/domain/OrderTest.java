package io.cb_demos.ecommerce.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    private Order order;
    private User user;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setQuantity(2);
        orderItem.setPrice(new BigDecimal("50.00"));

        order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-2024-001");
        order.setUser(user);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setStatus(Order.OrderStatus.PENDING);
        order.setShippingAddress("123 Test Street");
        order.setOrderDate(LocalDateTime.now());
    }

    @Test
    void getters_shouldReturnCorrectValues() {
        assertThat(order.getId()).isEqualTo(1L);
        assertThat(order.getOrderNumber()).isEqualTo("ORD-2024-001");
        assertThat(order.getUser()).isEqualTo(user);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(order.getShippingAddress()).isEqualTo("123 Test Street");
        assertThat(order.getOrderDate()).isNotNull();
    }

    @Test
    void setters_shouldUpdateValues() {
        // Given
        LocalDateTime newDate = LocalDateTime.now().plusDays(1);

        // When
        order.setOrderNumber("ORD-2024-002");
        order.setTotalAmount(new BigDecimal("200.00"));
        order.setStatus(Order.OrderStatus.SHIPPED);
        order.setShippingAddress("456 New Street");
        order.setOrderDate(newDate);

        // Then
        assertThat(order.getOrderNumber()).isEqualTo("ORD-2024-002");
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.SHIPPED);
        assertThat(order.getShippingAddress()).isEqualTo("456 New Street");
        assertThat(order.getOrderDate()).isEqualTo(newDate);
    }

    @Test
    void orderItems_shouldBeInitializedAsEmptyList() {
        // Given
        Order newOrder = new Order();

        // Then
        assertThat(newOrder.getOrderItems()).isNotNull();
        assertThat(newOrder.getOrderItems()).isEmpty();
    }

    @Test
    void addOrderItem_shouldAddToList() {
        // When
        order.getOrderItems().add(orderItem);

        // Then
        assertThat(order.getOrderItems()).hasSize(1);
        assertThat(order.getOrderItems()).contains(orderItem);
    }

    @Test
    void orderStatus_shouldSupportAllStatuses() {
        // Test all status transitions
        order.setStatus(Order.OrderStatus.PENDING);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);

        order.setStatus(Order.OrderStatus.CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);

        order.setStatus(Order.OrderStatus.PROCESSING);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PROCESSING);

        order.setStatus(Order.OrderStatus.SHIPPED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.SHIPPED);

        order.setStatus(Order.OrderStatus.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);

        order.setStatus(Order.OrderStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
    }

    @Test
    void equals_shouldComparById() {
        // Given
        Order order1 = new Order();
        order1.setId(1L);

        Order order2 = new Order();
        order2.setId(1L);

        Order order3 = new Order();
        order3.setId(2L);

        // Then
        assertThat(order1).isEqualTo(order2);
        assertThat(order1).isNotEqualTo(order3);
    }

    @Test
    void hashCode_shouldBeConsistent() {
        // Given
        Order order1 = new Order();
        order1.setId(1L);

        Order order2 = new Order();
        order2.setId(1L);

        // Then
        assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
    }

    @Test
    void toString_shouldIncludeOrderDetails() {
        // When
        String result = order.toString();

        // Then
        assertThat(result).contains("ORD-2024-001");
    }

    @Test
    void totalAmount_shouldHandleLargeValues() {
        // When
        order.setTotalAmount(new BigDecimal("999999.99"));

        // Then
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("999999.99"));
    }

    @Test
    void totalAmount_shouldHandleSmallValues() {
        // When
        order.setTotalAmount(new BigDecimal("0.01"));

        // Then
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    void orderDate_shouldBeBeforeNow() {
        // Given
        LocalDateTime pastDate = LocalDateTime.now().minusDays(5);

        // When
        order.setOrderDate(pastDate);

        // Then
        assertThat(order.getOrderDate()).isBefore(LocalDateTime.now());
    }

    @Test
    void multipleOrderItems_shouldCalculateTotalCorrectly() {
        // Given
        OrderItem item1 = new OrderItem();
        item1.setQuantity(2);
        item1.setPrice(new BigDecimal("25.00"));

        OrderItem item2 = new OrderItem();
        item2.setQuantity(3);
        item2.setPrice(new BigDecimal("10.00"));

        // When
        order.getOrderItems().add(item1);
        order.getOrderItems().add(item2);

        // Calculate total
        BigDecimal calculatedTotal = order.getOrderItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Then
        assertThat(calculatedTotal).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    void shippingAddress_shouldAllowLongAddresses() {
        // Given
        String longAddress = "Building 123, Floor 45, Apartment 678, " +
                "Very Long Street Name That Goes On And On, " +
                "City Name, State, Country, Postal Code 12345-6789";

        // When
        order.setShippingAddress(longAddress);

        // Then
        assertThat(order.getShippingAddress()).isEqualTo(longAddress);
    }

    @Test
    void orderNumber_shouldBeUnique() {
        // Given
        Order order1 = new Order();
        order1.setOrderNumber("ORD-001");

        Order order2 = new Order();
        order2.setOrderNumber("ORD-002");

        // Then
        assertThat(order1.getOrderNumber()).isNotEqualTo(order2.getOrderNumber());
    }

    @Test
    void newOrder_shouldHaveNullId() {
        // Given
        Order newOrder = new Order();

        // Then
        assertThat(newOrder.getId()).isNull();
    }

    @Test
    void orderItems_shouldSupportRemoval() {
        // Given
        order.getOrderItems().add(orderItem);
        assertThat(order.getOrderItems()).hasSize(1);

        // When
        order.getOrderItems().remove(orderItem);

        // Then
        assertThat(order.getOrderItems()).isEmpty();
    }

    @Test
    void user_shouldBeRequired() {
        // Given
        Order newOrder = new Order();

        // When
        newOrder.setUser(user);

        // Then
        assertThat(newOrder.getUser()).isNotNull();
        assertThat(newOrder.getUser()).isEqualTo(user);
    }

    @Test
    void orderStatusEnum_shouldHaveAllValues() {
        // Then
        assertThat(Order.OrderStatus.values()).contains(
                Order.OrderStatus.PENDING,
                Order.OrderStatus.CONFIRMED,
                Order.OrderStatus.PROCESSING,
                Order.OrderStatus.SHIPPED,
                Order.OrderStatus.DELIVERED,
                Order.OrderStatus.CANCELLED
        );
    }
}
