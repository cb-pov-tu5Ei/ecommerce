package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.CartItem;

import java.math.BigDecimal;
import java.util.List;

public interface CartService {

    void addToCart(Long productId, int quantity);

    void updateQuantity(Long productId, int quantity);

    void removeFromCart(Long productId);

    void clearCart();

    List<CartItem> getCartItems();

    BigDecimal calculateTotal();

    int getTotalItems();

    CartItem getCartItem(Long productId);
}
