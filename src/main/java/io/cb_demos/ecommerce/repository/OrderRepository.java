package io.cb_demos.ecommerce.repository;

import io.cb_demos.ecommerce.domain.Order;
import io.cb_demos.ecommerce.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserOrderByOrderDateDesc(User user, Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    Page<Order> findByUserAndStatus(User user, Order.OrderStatus status, Pageable pageable);

    java.util.List<Order> findByUserAndStatus(User user, Order.OrderStatus status);

    long countByUser(User user);

    java.util.List<Order> findByUser(User user);
}
