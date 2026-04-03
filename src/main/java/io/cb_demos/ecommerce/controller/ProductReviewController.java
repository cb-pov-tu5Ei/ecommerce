package io.cb_demos.ecommerce.controller;

import io.cb_demos.ecommerce.domain.ProductReview;
import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.service.ProductReviewService;
import io.cb_demos.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService reviewService;
    private final UserService userService;

    @GetMapping("/product/{productId}")
    public String getProductReviews(@PathVariable Long productId, Model model) {
        List<ProductReview> reviews = reviewService.getReviewsByProduct(productId);
        Double avgRating = reviewService.getAverageRating(productId);
        Long reviewCount = reviewService.getReviewCount(productId);

        model.addAttribute("reviews", reviews);
        model.addAttribute("averageRating", avgRating);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("productId", productId);

        return "reviews/product-reviews";
    }

    @PostMapping("/create")
    public String createReview(@RequestParam Long productId,
                               @RequestParam Integer rating,
                               @RequestParam(required = false) String comment,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getCurrentUser();
            reviewService.createReview(productId, user.getId(), rating, comment);
            redirectAttributes.addFlashAttribute("success", "Review added successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add review. Please try again.");
        }

        return "redirect:/products/" + productId;
    }

    @PostMapping("/delete/{reviewId}")
    public String deleteReview(@PathVariable Long reviewId,
                               @RequestParam Long productId,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getCurrentUser();
            reviewService.deleteReview(reviewId, user.getId());
            redirectAttributes.addFlashAttribute("success", "Review deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete review: " + e.getMessage());
        }

        return "redirect:/products/" + productId;
    }
}
