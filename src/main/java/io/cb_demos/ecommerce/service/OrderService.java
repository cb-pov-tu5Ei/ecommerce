package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.Order;
import io.cb_demos.ecommerce.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    Order createOrder(User user, String shippingAddress);

    Order createOrder(User user, String shippingAddress, String promoCode);

    Order findById(Long orderId);

    Page<Order> findByUser(User user, Pageable pageable);

    Page<Order> findUserOrders(User user, Pageable pageable);

    Order updateOrderStatus(Long orderId, Order.OrderStatus status);

    Order cancelOrder(Long orderId, User user);

    Order cancelOrder(Long orderId, User user, String reason);

    boolean canCancelOrder(Long orderId, User user);
}
