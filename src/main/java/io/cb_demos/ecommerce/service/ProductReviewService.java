package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.ProductReview;

import java.util.List;

public interface ProductReviewService {

    ProductReview createReview(Long productId, Long userId, Integer rating, String comment);

    ProductReview updateReview(Long reviewId, Long userId, Integer rating, String comment);

    void deleteReview(Long reviewId, Long userId);

    List<ProductReview> getReviewsByProduct(Long productId);

    List<ProductReview> getReviewsByUser(Long userId);

    Double getAverageRating(Long productId);

    Long getReviewCount(Long productId);

    List<ProductReview> getVerifiedReviewsByProduct(Long productId);
}
