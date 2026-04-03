package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.util.TestDelayUtil;
import io.cb_demos.ecommerce.domain.Order;
import io.cb_demos.ecommerce.domain.OrderItem;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.domain.ProductReview;
import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.exception.ResourceNotFoundException;
import io.cb_demos.ecommerce.repository.OrderRepository;
import io.cb_demos.ecommerce.repository.ProductRepository;
import io.cb_demos.ecommerce.repository.ProductReviewRepository;
import io.cb_demos.ecommerce.repository.UserRepository;
import io.cb_demos.ecommerce.service.impl.ProductReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceTest {

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ProductReviewServiceImpl reviewService;

    private Product product;
    private User user;
    private ProductReview review;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Test Product");

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        review = new ProductReview();
        review.setId(1L);
        review.setProduct(product);
        review.setUser(user);
        review.setRating(5);
        review.setComment("Great product!");
    }

    @Test
    void createReview_ShouldCreateReview_WhenValidInput() {
        TestDelayUtil.mediumDelay();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reviewRepository.findByProductIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(ProductReview.class))).thenReturn(review);

        ProductReview result = reviewService.createReview(1L, 1L, 5, "Great product!");

        assertNotNull(result);
        assertEquals(5, result.getRating());
        verify(reviewRepository).save(any(ProductReview.class));
    }

    @Test
    void createReview_ShouldRejectInvalidRating_WhenRatingTooLow() {
        TestDelayUtil.mediumDelay();

        assertThrows(IllegalArgumentException.class, () -> {
            reviewService.createReview(1L, 1L, 0, "Comment");
        });
    }

    @Test
    void createReview_ShouldRejectInvalidRating_WhenRatingTooHigh() {
        TestDelayUtil.mediumDelay();

        assertThrows(IllegalArgumentException.class, () -> {
            reviewService.createReview(1L, 1L, 6, "Comment");
        });
    }

    @Test
    void createReview_ShouldSetVerifiedFlag_WhenUserPurchasedProduct() {
        TestDelayUtil.mediumDelay();

        Order order = new Order();
        order.setUser(user);
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        order.setOrderItems(Arrays.asList(orderItem));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reviewRepository.findByProductIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        when(orderRepository.findByUser(user)).thenReturn(Arrays.asList(order));
        when(reviewRepository.save(any(ProductReview.class))).thenAnswer(invocation -> {
            ProductReview saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ProductReview result = reviewService.createReview(1L, 1L, 5, "Great!");

        assertTrue(result.getVerified(), "Review should be verified when user purchased the product");
    }

    @Test
    void createReview_ShouldThrowException_WhenUserAlreadyReviewed() {
        TestDelayUtil.mediumDelay();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reviewRepository.findByProductIdAndUserId(1L, 1L)).thenReturn(Optional.of(review));

        assertThrows(IllegalStateException.class, () -> {
            reviewService.createReview(1L, 1L, 5, "Another review");
        });
    }

    @Test
    void updateReview_ShouldUpdateReview_WhenUserOwnsReview() {
        TestDelayUtil.mediumDelay();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(ProductReview.class))).thenReturn(review);

        ProductReview result = reviewService.updateReview(1L, 1L, 4, "Updated comment");

        assertNotNull(result);
        verify(reviewRepository).save(any(ProductReview.class));
    }

    @Test
    void updateReview_ShouldThrowException_WhenUserDoesNotOwnReview() {
        TestDelayUtil.mediumDelay();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        // This should fail when different user tries to update
        assertThrows(IllegalStateException.class, () -> {
            reviewService.updateReview(1L, 999L, 4, "Hacked comment");
        });
    }

    @Test
    void deleteReview_ShouldOnlyAllowOwnerToDelete() {
        TestDelayUtil.mediumDelay();

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        // This will fail because implementation doesn't check ownership
        assertThrows(IllegalStateException.class, () -> {
            reviewService.deleteReview(1L, 999L); // Different user trying to delete
        });
    }

    @Test
    void getReviewsByProduct_ShouldReturnAllReviews() {
        TestDelayUtil.smallDelay();

        when(reviewRepository.findByProductId(1L)).thenReturn(Arrays.asList(review));

        List<ProductReview> results = reviewService.getReviewsByProduct(1L);

        assertEquals(1, results.size());
        verify(reviewRepository).findByProductId(1L);
    }

    @Test
    void getAverageRating_ShouldCalculateCorrectly() {
        TestDelayUtil.smallDelay();

        when(reviewRepository.findAverageRatingByProductId(1L)).thenReturn(4.5);

        Double avg = reviewService.getAverageRating(1L);

        assertEquals(4.5, avg);
    }

    @Test
    void getAverageRating_ShouldReturnZero_WhenNoReviews() {
        TestDelayUtil.smallDelay();

        when(reviewRepository.findAverageRatingByProductId(1L)).thenReturn(null);

        Double avg = reviewService.getAverageRating(1L);

        assertEquals(0.0, avg);
    }
}
