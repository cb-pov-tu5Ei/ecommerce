package io.cb_demos.ecommerce.service.impl;

import io.cb_demos.ecommerce.domain.Category;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.exception.InsufficientStockException;
import io.cb_demos.ecommerce.exception.ProductNotFoundException;
import io.cb_demos.ecommerce.repository.CategoryRepository;
import io.cb_demos.ecommerce.repository.ProductRepository;
import io.cb_demos.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findAllProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findByCategory(Long categoryId, Pageable pageable) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
        return productRepository.findByCategoryAndActiveTrue(category, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String query, Pageable pageable) {
        return productRepository.searchProducts(query, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findFeaturedProducts() {
        return productRepository.findTop8ByActiveTrueOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInStock(Long productId, int quantity) {
        Product product = findById(productId);
        return product.getStockQuantity() >= quantity;
    }

    @Override
    @Transactional
    public void updateStock(Long productId, int quantity) {
        Product product = findById(productId);

        int newStock = product.getStockQuantity() + quantity;
        if (newStock < 0) {
            throw new InsufficientStockException(
                    product.getName(),
                    Math.abs(quantity),
                    product.getStockQuantity()
            );
        }

        product.setStockQuantity(newStock);
        productRepository.save(product);
        log.info("Updated stock for product {}: {} -> {}", productId, product.getStockQuantity(), newStock);
    }
}
