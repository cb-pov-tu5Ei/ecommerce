package io.cb_demos.ecommerce.repository;

import io.cb_demos.ecommerce.util.TestDelayUtil;
import io.cb_demos.ecommerce.domain.Category;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.domain.ProductReview;
import io.cb_demos.ecommerce.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProductReviewRepositoryTest {

    @Autowired
    private ProductReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Product product;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("Electronics");
        category = categoryRepository.save(category);

        product = new Product();
        product.setName("Test Product");
        product.setDescription("Description");
        product.setPrice(BigDecimal.valueOf(99.99));
        product.setStockQuantity(10);
        product.setCategory(category);
        product = productRepository.save(product);

        user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("password");
        user1.setEmail("user1@test.com");
        user1.setFirstName("User");
        user1.setLastName("One");
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setUsername("user2");
        user2.setPassword("password");
        user2.setEmail("user2@test.com");
        user2.setFirstName("User");
        user2.setLastName("Two");
        user2 = userRepository.save(user2);
    }

    @Test
    void findByProductId_ShouldReturnAllReviewsForProduct() {
        TestDelayUtil.mediumDelay();

        ProductReview review1 = createReview(product, user1, 5, "Great!");
        ProductReview review2 = createReview(product, user2, 4, "Good");

        List<ProductReview> reviews = reviewRepository.findByProductId(product.getId());

        assertEquals(2, reviews.size());
    }

    @Test
    void findByUserId_ShouldReturnAllReviewsByUser() {
        TestDelayUtil.mediumDelay();

        ProductReview review1 = createReview(product, user1, 5, "Great!");

        List<ProductReview> reviews = reviewRepository.findByUserId(user1.getId());

        assertEquals(1, reviews.size());
        assertEquals("Great!", reviews.get(0).getComment());
    }

    @Test
    void findByProductIdAndUserId_ShouldReturnReview_WhenExists() {
        TestDelayUtil.mediumDelay();

        ProductReview review = createReview(product, user1, 5, "Great!");

        Optional<ProductReview> found = reviewRepository.findByProductIdAndUserId(
                product.getId(), user1.getId());

        assertTrue(found.isPresent());
        assertEquals(5, found.get().getRating());
    }

    @Test
    void findByProductIdAndUserId_ShouldReturnEmpty_WhenNotExists() {
        TestDelayUtil.smallDelay();

        Optional<ProductReview> found = reviewRepository.findByProductIdAndUserId(
                product.getId(), user1.getId());

        assertFalse(found.isPresent());
    }

    @Test
    void findAverageRatingByProductId_ShouldCalculateCorrectly() {
        TestDelayUtil.mediumDelay();

        createReview(product, user1, 5, "Great!");
        createReview(product, user2, 3, "OK");

        Double avgRating = reviewRepository.findAverageRatingByProductId(product.getId());

        assertNotNull(avgRating);
        assertEquals(4.0, avgRating, 0.01);
    }

    @Test
    void countByProductId_ShouldReturnCorrectCount() {
        TestDelayUtil.smallDelay();

        createReview(product, user1, 5, "Great!");
        createReview(product, user2, 4, "Good");

        Long count = reviewRepository.countByProductId(product.getId());

        assertEquals(2, count);
    }

    @Test
    void findByProductIdAndVerifiedTrue_ShouldReturnOnlyVerifiedReviews() {
        TestDelayUtil.mediumDelay();

        ProductReview verified = createReview(product, user1, 5, "Verified!");
        verified.setVerified(true);
        reviewRepository.save(verified);

        ProductReview unverified = createReview(product, user2, 4, "Not verified");
        unverified.setVerified(false);
        reviewRepository.save(unverified);

        List<ProductReview> verifiedReviews = reviewRepository.findByProductIdAndVerifiedTrue(product.getId());

        assertEquals(1, verifiedReviews.size());
        assertTrue(verifiedReviews.get(0).getVerified());
    }

    private ProductReview createReview(Product product, User user, Integer rating, String comment) {
        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(rating);
        review.setComment(comment);
        review.setVerified(false);
        return reviewRepository.save(review);
    }
}
