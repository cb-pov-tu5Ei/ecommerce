package io.cb_demos.ecommerce.controller;

import io.cb_demos.ecommerce.domain.Order;
import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.exception.InsufficientStockException;
import io.cb_demos.ecommerce.exception.InvalidOrderStateException;
import io.cb_demos.ecommerce.exception.MinimumOrderValueException;
import io.cb_demos.ecommerce.service.CartService;
import io.cb_demos.ecommerce.service.OrderService;
import io.cb_demos.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;

    @GetMapping("/checkout")
    public String showCheckout(Model model) {
        if (cartService.getTotalItems() == 0) {
            return "redirect:/cart";
        }

        User user = userService.getCurrentUser();
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("cartTotal", cartService.calculateTotal());
        model.addAttribute("user", user);
        return "orders/checkout";
    }

    @PostMapping("/place")
    public String processOrder(@RequestParam String shippingAddress,
                               RedirectAttributes redirectAttributes) {
        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Shipping address is required");
            return "redirect:/orders/checkout";
        }

        if (shippingAddress.length() > 255) {
            redirectAttributes.addFlashAttribute("error", "Shipping address is too long");
            return "redirect:/orders/checkout";
        }

        if (cartService.getTotalItems() == 0) {
            redirectAttributes.addFlashAttribute("error", "Your cart is empty!");
            return "redirect:/cart";
        }

        try {
            User user = userService.getCurrentUser();
            Order order = orderService.createOrder(user, shippingAddress);
            log.info("Order placed successfully: {}", order.getOrderNumber());
            return "redirect:/orders/confirmation/" + order.getId();
        } catch (IllegalStateException e) {
            log.error("Order failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        } catch (InsufficientStockException e) {
            log.error("Order failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        } catch (MinimumOrderValueException e) {
            log.error("Order failed - minimum not met: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        } catch (Exception e) {
            log.error("Error processing order", e);
            redirectAttributes.addFlashAttribute("error", "Failed to process order. Please try again.");
            return "redirect:/cart";
        }
    }

    @GetMapping("/confirmation/{orderId}")
    public String orderConfirmation(@PathVariable Long orderId, Model model) {
        Order order = orderService.findById(orderId);
        User currentUser = userService.getCurrentUser();

        if (!order.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        return "orders/confirmation";
    }

    @GetMapping
    public String listOrders(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            Model model) {
        User user = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        var ordersPage = orderService.findUserOrders(user, pageable);
        model.addAttribute("orders", ordersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        return "orders/list";
    }

    @GetMapping("/history")
    public String orderHistory(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        User user = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        var ordersPage = orderService.findUserOrders(user, pageable);
        model.addAttribute("orders", ordersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        return "orders/list";
    }

    @GetMapping("/{orderId}")
    public String orderDetails(@PathVariable Long orderId, Model model) {
        Order order = orderService.findById(orderId);
        User currentUser = userService.getCurrentUser();

        if (!order.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        return "orders/detail";
    }

    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable Long orderId,
                             @RequestParam(required = false) String reason,
                             RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser();
            orderService.cancelOrder(orderId, currentUser, reason);
            redirectAttributes.addFlashAttribute("success", "Order cancelled successfully");
        } catch (InvalidOrderStateException e) {
            log.error("Error cancelling order: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error cancelling order", e);
            redirectAttributes.addFlashAttribute("error", "Failed to cancel order");
        }
        return "redirect:/orders/" + orderId;
    }
}
