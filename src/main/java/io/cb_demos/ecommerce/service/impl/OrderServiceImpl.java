package io.cb_demos.ecommerce.service.impl;

import io.cb_demos.ecommerce.domain.CartItem;
import io.cb_demos.ecommerce.domain.Order;
import io.cb_demos.ecommerce.domain.OrderItem;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.exception.InsufficientStockException;
import io.cb_demos.ecommerce.exception.InvalidOrderStateException;
import io.cb_demos.ecommerce.exception.MinimumOrderValueException;
import io.cb_demos.ecommerce.exception.OrderNotFoundException;
import io.cb_demos.ecommerce.repository.OrderRepository;
import io.cb_demos.ecommerce.domain.PromoCode;
import io.cb_demos.ecommerce.domain.ShippingMethod;
import io.cb_demos.ecommerce.service.CartService;
import io.cb_demos.ecommerce.service.OrderService;
import io.cb_demos.ecommerce.service.ProductService;
import io.cb_demos.ecommerce.service.PromoCodeService;
import io.cb_demos.ecommerce.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final ProductService productService;
    private final PromoCodeService promoCodeService;
    private final ShippingService shippingService;

    @Override
    @Transactional
    public Order createOrder(User user, String shippingAddress) {
        List<CartItem> cartItems = cartService.getCartItems();

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot create order with empty cart");
        }

        // FIXED: Validate minimum order value BEFORE checking stock (fail fast)
        BigDecimal cartTotal = cartItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // NOTE: Hardcoded minimum value - could be externalized to configuration in the future
        BigDecimal minimumOrderValue = new BigDecimal("20.00");
        if (cartTotal.compareTo(minimumOrderValue) < 0) {
            throw new MinimumOrderValueException(cartTotal, minimumOrderValue);
        }

        // Verify stock availability for all items
        for (CartItem cartItem : cartItems) {
            if (!productService.isInStock(cartItem.getProductId(), cartItem.getQuantity())) {
                Product product = productService.findById(cartItem.getProductId());
                throw new InsufficientStockException(
                        product.getName(),
                        cartItem.getQuantity(),
                        product.getStockQuantity()
                );
            }
        }

        // Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setShippingAddress(shippingAddress);

        // Create order items and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = productService.findById(cartItem.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtOrder(product.getPrice());
            orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());

            // Reduce stock
            productService.updateStock(product.getId(), -cartItem.getQuantity());
        }

        order.setOrderItems(orderItems);

        // Add shipping cost
        ShippingMethod shippingMethod = shippingService.getDefaultShippingMethod();
        BigDecimal shippingCost = shippingService.calculateShippingCost(totalAmount, shippingMethod);
        order.setShippingMethod(shippingMethod);
        order.setShippingCost(shippingCost);

        order.setTotalAmount(totalAmount.add(shippingCost));

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully: {} with shipping: {}", savedOrder.getOrderNumber(), shippingCost);

        // Clear cart after successful order
        cartService.clearCart();

        return savedOrder;
    }

    @Override
    @Transactional
    public Order createOrder(User user, String shippingAddress, String promoCode) {
        List<CartItem> cartItems = cartService.getCartItems();

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot create order with empty cart");
        }

        // FIXED: Validate minimum order value BEFORE checking stock (fail fast)
        BigDecimal cartTotal = cartItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // NOTE: Hardcoded minimum value - could be externalized to configuration in the future
        BigDecimal minimumOrderValue = new BigDecimal("20.00");
        if (cartTotal.compareTo(minimumOrderValue) < 0) {
            throw new MinimumOrderValueException(cartTotal, minimumOrderValue);
        }

        // Verify stock availability for all items
        for (CartItem cartItem : cartItems) {
            if (!productService.isInStock(cartItem.getProductId(), cartItem.getQuantity())) {
                Product product = productService.findById(cartItem.getProductId());
                throw new InsufficientStockException(
                        product.getName(),
                        cartItem.getQuantity(),
                        product.getStockQuantity()
                );
            }
        }

        // Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setShippingAddress(shippingAddress);

        // Create order items and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = productService.findById(cartItem.getProductId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtOrder(product.getPrice());
            orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());

            // Reduce stock
            productService.updateStock(product.getId(), -cartItem.getQuantity());
        }

        order.setOrderItems(orderItems);

        // Apply promo code discount if valid
        BigDecimal discount = promoCodeService.calculateDiscount(totalAmount, promoCode);
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            promoCodeService.findByCode(promoCode).ifPresent(order::setPromoCode);
            order.setDiscountAmount(discount);
        }

        // Add shipping cost
        ShippingMethod shippingMethod = shippingService.getDefaultShippingMethod();
        BigDecimal shippingCost = shippingService.calculateShippingCost(totalAmount, shippingMethod);
        order.setShippingMethod(shippingMethod);
        order.setShippingCost(shippingCost);

        order.setTotalAmount(totalAmount.subtract(discount).add(shippingCost));

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully: {} with promo discount: {} and shipping: {}",
                savedOrder.getOrderNumber(), discount, shippingCost);

        // Clear cart after successful order
        cartService.clearCart();

        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public Order findById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> findByUser(User user, Pageable pageable) {
        return orderRepository.findByUserOrderByOrderDateDesc(user, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> findUserOrders(User user, Pageable pageable) {
        return orderRepository.findByUserOrderByOrderDateDesc(user, pageable);
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = findById(orderId);
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} status updated to {}", orderId, status);
        return updatedOrder;
    }
    @Override
    public Order cancelOrder(Long orderId, User user) {
        return cancelOrder(orderId, user, null);
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId, User user, String reason) {
        Order order = findById(orderId);

        // Validate user ownership
        if (!order.getUser().getId().equals(user.getId())) {
            throw new InvalidOrderStateException("User does not own this order");
        }

        // Check if order is already cancelled
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException(
                orderId,
                order.getStatus(),
                "Order is already cancelled"
            );
        }

        if (!order.isCancellable()) {
            throw new InvalidOrderStateException(
                orderId,
                order.getStatus(),
                "Order cannot be cancelled in current state"
            );
        }

        // Restore stock for all order items
        for (OrderItem item : order.getOrderItems()) {
            productService.updateStock(item.getProduct().getId(), item.getQuantity());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());

        if (reason != null && !reason.isBlank()) {
            order.setCancellationReason(reason);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} cancelled", orderId);
        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canCancelOrder(Long orderId, User user) {
        Order order = findById(orderId);

        // Validate user ownership
        if (!order.getUser().getId().equals(user.getId())) {
            return false;
        }

        return order.isCancellable();
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 9000) + 1000;
        return "ORD-" + timestamp + "-" + random;
    }
}
