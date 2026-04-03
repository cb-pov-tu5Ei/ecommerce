package io.cb_demos.ecommerce.repository;

import io.cb_demos.ecommerce.domain.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductId(Long productId);

    List<ProductReview> findByUserId(Long userId);

    Optional<ProductReview> findByProductIdAndUserId(Long productId, Long userId);

    @Query("SELECT AVG(pr.rating) FROM ProductReview pr WHERE pr.product.id = :productId")
    Double findAverageRatingByProductId(Long productId);

    Long countByProductId(Long productId);

    List<ProductReview> findByProductIdAndVerifiedTrue(Long productId);
}
