package io.cb_demos.ecommerce.service.impl;

import io.cb_demos.ecommerce.domain.CartItem;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.exception.InsufficientStockException;
import io.cb_demos.ecommerce.service.CartService;
import io.cb_demos.ecommerce.service.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@SessionScope
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final ProductService productService;
    private final HttpSession session;

    private static final String CART_SESSION_KEY = "shopping_cart";

    @SuppressWarnings("unchecked")
    private Map<Long, CartItem> getCart() {
        Map<Long, CartItem> cart = (Map<Long, CartItem>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    @Override
    public void addToCart(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Product product = productService.findById(productId);
        Map<Long, CartItem> cart = getCart();

        CartItem existingItem = cart.get(productId);
        int newQuantity = existingItem != null ? existingItem.getQuantity() + quantity : quantity;

        // Note: Stock validation is done at checkout, not when adding to cart
        CartItem cartItem = new CartItem(
                product.getId(),
                product.getName(),
                product.getPrice(),
                newQuantity,
                product.getImageUrl()
        );

        cart.put(productId, cartItem);
        log.info("Added to cart: {} x{}", product.getName(), quantity);
    }

    @Override
    public void updateQuantity(Long productId, int quantity) {
        if (quantity <= 0) {
            removeFromCart(productId);
            return;
        }

        Map<Long, CartItem> cart = getCart();

        if (!cart.containsKey(productId)) {
            log.warn("Attempted to update quantity for product not in cart: {}", productId);
            return;
        }

        Product product = productService.findById(productId);

        // Note: Stock validation is done at checkout, not when updating cart
        CartItem cartItem = cart.get(productId);
        cartItem.setQuantity(quantity);
        log.info("Updated cart quantity: {} x{}", product.getName(), quantity);
    }

    @Override
    public void removeFromCart(Long productId) {
        Map<Long, CartItem> cart = getCart();
        CartItem removed = cart.remove(productId);
        if (removed != null) {
            log.info("Removed from cart: {}", removed.getProductName());
        }
    }

    @Override
    public void clearCart() {
        session.removeAttribute(CART_SESSION_KEY);
        log.info("Cart cleared");
    }

    @Override
    public List<CartItem> getCartItems() {
        return new ArrayList<>(getCart().values());
    }

    @Override
    public BigDecimal calculateTotal() {
        return getCart().values().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public int getTotalItems() {
        return getCart().values().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    @Override
    public CartItem getCartItem(Long productId) {
        return getCart().get(productId);
    }
}
