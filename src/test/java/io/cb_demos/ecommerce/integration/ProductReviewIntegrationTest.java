package io.cb_demos.ecommerce.integration;

import io.cb_demos.ecommerce.util.TestDelayUtil;
import io.cb_demos.ecommerce.domain.*;
import io.cb_demos.ecommerce.repository.*;
import io.cb_demos.ecommerce.service.ProductReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProductReviewIntegrationTest {

    @Autowired
    private ProductReviewService reviewService;

    @Autowired
    private ProductReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Product product;
    private User buyer;
    private User nonBuyer;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.findByName("Electronics")
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName("Electronics");
                    return categoryRepository.save(newCategory);
                });

        // Create product
        product = new Product();
        product.setName("Laptop");
        product.setDescription("Gaming laptop");
        product.setPrice(BigDecimal.valueOf(1299.99));
        product.setStockQuantity(5);
        product.setCategory(category);
        product = productRepository.save(product);

        // Create buyers
        buyer = createUser("buyer", "buyer@test.com", "Verified", "Buyer");
        nonBuyer = createUser("nonbuyer", "nonbuyer@test.com", "Non", "Buyer");

        // Create an order for the buyer
        Order order = new Order();
        order.setUser(buyer);
        order.setOrderNumber("ORD-TEST-001");
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(BigDecimal.valueOf(1299.99));
        order.setStatus(Order.OrderStatus.DELIVERED);
        order.setShippingAddress("123 Test St");
        order.setShippingMethod(ShippingMethod.STANDARD);
        order.setShippingCost(BigDecimal.ZERO);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(1);
        orderItem.setPriceAtOrder(product.getPrice());
        orderItem.setSubtotal(product.getPrice());

        order.setOrderItems(Arrays.asList(orderItem));
        orderRepository.save(order);
    }

    @Test
    @WithMockUser(username = "buyer")
    void completeReviewLifecycle_CreateUpdateDelete() {
        TestDelayUtil.extraLargeDelay();

        // Create review
        ProductReview created = reviewService.createReview(
                product.getId(), buyer.getId(), 5, "Excellent laptop!");

        assertNotNull(created.getId());
        assertEquals(5, created.getRating());
        assertEquals("Excellent laptop!", created.getComment());

        // Update review
        ProductReview updated = reviewService.updateReview(
                created.getId(), buyer.getId(), 4, "Good laptop, but pricey");

        assertEquals(4, updated.getRating());
        assertEquals("Good laptop, but pricey", updated.getComment());

        // Delete review
        reviewService.deleteReview(created.getId(), buyer.getId());

        assertFalse(reviewRepository.findById(created.getId()).isPresent());
    }

    @Test
    @WithMockUser(username = "buyer")
    void createReview_ShouldMarkAsVerified_WhenUserPurchasedProduct() {
        TestDelayUtil.largeDelay();

        ProductReview review = reviewService.createReview(
                product.getId(), buyer.getId(), 5, "Verified purchase!");

        // This will fail because implementation doesn't check purchase history
        assertTrue(review.getVerified(),
                "Review should be verified when user has purchased the product");
    }

    @Test
    @WithMockUser(username = "nonbuyer")
    void createReview_ShouldNotBeVerified_WhenUserDidNotPurchase() {
        TestDelayUtil.largeDelay();

        ProductReview review = reviewService.createReview(
                product.getId(), nonBuyer.getId(), 3, "Not verified");

        assertFalse(review.getVerified(),
                "Review should not be verified when user hasn't purchased the product");
    }

    @Test
    @WithMockUser(username = "buyer")
    void createReview_ShouldRejectInvalidRatings() {
        TestDelayUtil.mediumDelay();

        // Rating 0 should be rejected
        assertThrows(IllegalArgumentException.class, () -> {
            reviewService.createReview(product.getId(), buyer.getId(), 0, "Zero rating");
        });

        // Rating 6 should be rejected
        assertThrows(IllegalArgumentException.class, () -> {
            reviewService.createReview(product.getId(), buyer.getId(), 6, "Six rating");
        });
    }

    @Test
    @WithMockUser(username = "buyer")
    void createReview_ShouldPreventDuplicateReviews() {
        TestDelayUtil.mediumDelay();

        reviewService.createReview(product.getId(), buyer.getId(), 5, "First review");

        assertThrows(IllegalStateException.class, () -> {
            reviewService.createReview(product.getId(), buyer.getId(), 4, "Duplicate review");
        });
    }

    @Test
    @WithMockUser(username = "buyer")
    void updateReview_ShouldPreventUnauthorizedUpdate() {
        TestDelayUtil.mediumDelay();

        ProductReview review = reviewService.createReview(
                product.getId(), buyer.getId(), 5, "Original");

        // Different user trying to update
        assertThrows(IllegalStateException.class, () -> {
            reviewService.updateReview(review.getId(), nonBuyer.getId(), 1, "Hacked");
        });
    }

    @Test
    @WithMockUser(username = "buyer")
    void deleteReview_ShouldPreventUnauthorizedDelete() {
        TestDelayUtil.mediumDelay();

        ProductReview review = reviewService.createReview(
                product.getId(), buyer.getId(), 5, "Original");

        // Different user trying to delete - This will fail because implementation doesn't check
        assertThrows(IllegalStateException.class, () -> {
            reviewService.deleteReview(review.getId(), nonBuyer.getId());
        });
    }

    @Test
    @WithMockUser(username = "buyer")
    void getAverageRating_ShouldCalculateCorrectly() {
        TestDelayUtil.mediumDelay();

        reviewService.createReview(product.getId(), buyer.getId(), 5, "Great!");
        reviewService.createReview(product.getId(), nonBuyer.getId(), 3, "OK");

        Double avgRating = reviewService.getAverageRating(product.getId());

        assertEquals(4.0, avgRating, 0.01);
    }

    @Test
    @WithMockUser(username = "buyer")
    void getVerifiedReviews_ShouldOnlyReturnVerifiedReviews() {
        TestDelayUtil.largeDelay();

        // Buyer's review should be verified
        reviewService.createReview(product.getId(), buyer.getId(), 5, "Verified!");

        // Non-buyer's review should not be verified
        reviewService.createReview(product.getId(), nonBuyer.getId(), 4, "Not verified");

        List<ProductReview> verifiedReviews = reviewService.getVerifiedReviewsByProduct(product.getId());

        // This will fail because implementation doesn't set verified flag
        assertEquals(1, verifiedReviews.size(),
                "Should only return one verified review");
        assertTrue(verifiedReviews.get(0).getVerified());
    }

    @Test
    @WithMockUser(username = "buyer")
    void getReviewCount_ShouldReturnCorrectCount() {
        TestDelayUtil.smallDelay();

        reviewService.createReview(product.getId(), buyer.getId(), 5, "Review 1");
        reviewService.createReview(product.getId(), nonBuyer.getId(), 4, "Review 2");

        Long count = reviewService.getReviewCount(product.getId());

        assertEquals(2, count);
    }

    private User createUser(String username, String email, String firstName, String lastName) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password123");
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        return userRepository.save(user);
    }
}
