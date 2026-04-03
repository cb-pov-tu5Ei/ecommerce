package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    Page<Product> findAllProducts(Pageable pageable);

    Product findById(Long id);

    Page<Product> findByCategory(Long categoryId, Pageable pageable);

    Page<Product> searchProducts(String query, Pageable pageable);

    List<Product> findFeaturedProducts();

    boolean isInStock(Long productId, int quantity);

    void updateStock(Long productId, int quantity);
}
