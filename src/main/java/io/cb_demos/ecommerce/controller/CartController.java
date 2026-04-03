package io.cb_demos.ecommerce.controller;

import io.cb_demos.ecommerce.exception.InsufficientStockException;
import io.cb_demos.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @GetMapping
    public String viewCart(Model model) {
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("total", cartService.calculateTotal());
        model.addAttribute("totalItems", cartService.getTotalItems());
        return "cart/view";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            RedirectAttributes redirectAttributes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        try {
            cartService.addToCart(productId, quantity);
            redirectAttributes.addFlashAttribute("success", "Product added to cart successfully!");
        } catch (InsufficientStockException e) {
            log.error("Failed to add to cart: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/products/" + productId;
        }
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateQuantity(@RequestParam Long productId,
                                  @RequestParam int quantity,
                                  RedirectAttributes redirectAttributes) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        try {
            cartService.updateQuantity(productId, quantity);
            redirectAttributes.addFlashAttribute("success", "Cart updated successfully!");
        } catch (InsufficientStockException e) {
            log.error("Failed to update cart: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Invalid cart update: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove/{productId}")
    public String removeFromCart(@PathVariable Long productId,
                                  RedirectAttributes redirectAttributes) {
        cartService.removeFromCart(productId);
        redirectAttributes.addFlashAttribute("success", "Product removed from cart!");
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(RedirectAttributes redirectAttributes) {
        cartService.clearCart();
        redirectAttributes.addFlashAttribute("success", "Cart cleared!");
        return "redirect:/cart";
    }
}
