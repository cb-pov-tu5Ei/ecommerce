package io.cb_demos.ecommerce.integration;

import io.cb_demos.ecommerce.domain.Category;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.repository.CategoryRepository;
import io.cb_demos.ecommerce.repository.ProductRepository;
import io.cb_demos.ecommerce.service.ProductService;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ProductSearchIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;
    private Category clothing;
    private Category books;

    @BeforeEach
    void setUp() {
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();

        // Create categories
        electronics = new Category();
        electronics.setName("Electronics");
        electronics.setDescription("Electronic devices and accessories");
        electronics = categoryRepository.save(electronics);

        clothing = new Category();
        clothing.setName("Clothing");
        clothing.setDescription("Apparel and fashion");
        clothing = categoryRepository.save(clothing);

        books = new Category();
        books.setName("Books");
        books.setDescription("Physical and digital books");
        books = categoryRepository.save(books);

        // Create electronics products
        createProduct("Laptop Pro 15", "High-performance laptop", new BigDecimal("1299.99"), 10, electronics, true);
        createProduct("Wireless Mouse", "Ergonomic wireless mouse", new BigDecimal("29.99"), 50, electronics, true);
        createProduct("USB-C Cable", "Fast charging USB-C cable", new BigDecimal("12.99"), 100, electronics, true);
        createProduct("Mechanical Keyboard", "RGB mechanical keyboard", new BigDecimal("89.99"), 25, electronics, true);
        createProduct("4K Monitor", "27-inch 4K display", new BigDecimal("399.99"), 15, electronics, true);

        // Create clothing products
        createProduct("Cotton T-Shirt", "Comfortable cotton tee", new BigDecimal("19.99"), 200, clothing, true);
        createProduct("Denim Jeans", "Classic fit denim jeans", new BigDecimal("49.99"), 100, clothing, true);
        createProduct("Winter Jacket", "Warm winter jacket", new BigDecimal("129.99"), 30, clothing, true);

        // Create books products
        createProduct("Java Programming", "Learn Java from scratch", new BigDecimal("39.99"), 50, books, true);
        createProduct("Spring Boot Guide", "Mastering Spring Boot", new BigDecimal("44.99"), 40, books, true);
        createProduct("Algorithm Design", "Advanced algorithms", new BigDecimal("54.99"), 35, books, true);

        // Create inactive product
        createProduct("Old Laptop", "Discontinued laptop model", new BigDecimal("499.99"), 0, electronics, false);
    }

    private Product createProduct(String name, String description, BigDecimal price, int stock, Category category, boolean active) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setCategory(category);
        product.setActive(active);
        return productRepository.save(product);
    }

    @Test
    void searchProducts_shouldFindByName() {
        TestDelayUtil.largeDelay();
        // When
        Page<Product> results = productService.searchProducts("Laptop", PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).contains("Laptop Pro");
    }

    @Test
    void searchProducts_shouldFindByDescription() {
        TestDelayUtil.mediumDelay();
        // When
        Page<Product> results = productService.searchProducts("ergonomic", PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).isEqualTo("Wireless Mouse");
    }

    @Test
    void searchProducts_shouldBeCaseInsensitive() {
        TestDelayUtil.massiveDelay();
        // When
        Page<Product> upperResults = productService.searchProducts("LAPTOP", PageRequest.of(0, 10));
        Page<Product> lowerResults = productService.searchProducts("laptop", PageRequest.of(0, 10));
        Page<Product> mixedResults = productService.searchProducts("LaPtOp", PageRequest.of(0, 10));

        // Then
        assertThat(upperResults.getContent()).hasSize(1);
        assertThat(lowerResults.getContent()).hasSize(1);
        assertThat(mixedResults.getContent()).hasSize(1);
    }

    @Test
    void searchProducts_shouldFindPartialMatches() {
        // When
        Page<Product> results = productService.searchProducts("USB", PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).contains("USB-C");
    }

    @Test
    void searchProducts_shouldReturnEmptyForNoMatch() {
        // When
        Page<Product> results = productService.searchProducts("NonExistentProduct", PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).isEmpty();
        assertThat(results.getTotalElements()).isZero();
    }

    @Test
    void findByCategory_shouldReturnOnlyElectronics() {
        TestDelayUtil.largeDelay();
        // When
        Page<Product> results = productService.findByCategory(electronics.getId(), PageRequest.of(0, 20));

        // Then
        assertThat(results.getContent()).hasSize(5);
        assertThat(results.getContent())
                .allMatch(p -> p.getCategory().getName().equals("Electronics"));
    }

    @Test
    void findByCategory_shouldReturnOnlyClothing() {
        TestDelayUtil.mediumDelay();
        // When
        Page<Product> results = productService.findByCategory(clothing.getId(), PageRequest.of(0, 20));

        // Then
        assertThat(results.getContent()).hasSize(3);
        assertThat(results.getContent())
                .allMatch(p -> p.getCategory().getName().equals("Clothing"));
    }

    @Test
    void findByCategory_shouldReturnOnlyBooks() {
        TestDelayUtil.mediumDelay();
        // When
        Page<Product> results = productService.findByCategory(books.getId(), PageRequest.of(0, 20));

        // Then
        assertThat(results.getContent()).hasSize(3);
        assertThat(results.getContent())
                .allMatch(p -> p.getCategory().getName().equals("Books"));
    }

    @Test
    void findAllProducts_shouldExcludeInactiveProducts() {
        TestDelayUtil.mediumDelay();
        // When
        Page<Product> results = productService.findAllProducts(PageRequest.of(0, 20));

        // Then
        assertThat(results.getContent()).hasSize(11); // All except the inactive one
        assertThat(results.getContent()).allMatch(Product::isActive);
    }

    @Test
    void findAllProducts_shouldHandlePagination() {
        TestDelayUtil.extraLargeDelay();
        // When
        Page<Product> page1 = productService.findAllProducts(PageRequest.of(0, 5));
        Page<Product> page2 = productService.findAllProducts(PageRequest.of(1, 5));
        Page<Product> page3 = productService.findAllProducts(PageRequest.of(2, 5));

        // Then
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page2.getContent()).hasSize(5);
        assertThat(page3.getContent()).hasSize(1);
        assertThat(page1.getTotalPages()).isEqualTo(3);
        assertThat(page1.getTotalElements()).isEqualTo(11);
    }

    @Test
    void searchProducts_shouldFindMultipleMatches() {
        // When - Search for "Java" or "Spring" in books
        Page<Product> results = productService.searchProducts("Java", PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).contains("Java");
    }

    @Test
    void findFeaturedProducts_shouldReturnLatestActiveProducts() {
        // When
        List<Product> featured = productService.findFeaturedProducts();

        // Then
        assertThat(featured).isNotEmpty();
        assertThat(featured).allMatch(Product::isActive);
        assertThat(featured).hasSizeLessThanOrEqualTo(8);
    }

    @Test
    void isInStock_shouldReturnTrueForAvailableProducts() {
        // Given
        Product laptop = productRepository.findAll().stream()
                .filter(p -> p.getName().equals("Laptop Pro 15"))
                .findFirst()
                .orElseThrow();

        // When & Then
        assertThat(productService.isInStock(laptop.getId(), 5)).isTrue();
        assertThat(productService.isInStock(laptop.getId(), 10)).isTrue();
        assertThat(productService.isInStock(laptop.getId(), 15)).isFalse();
    }

    @Test
    void updateStock_shouldAdjustInventoryCorrectly() {
        TestDelayUtil.extraLargeDelay();
        // Given
        Product mouse = productRepository.findAll().stream()
                .filter(p -> p.getName().equals("Wireless Mouse"))
                .findFirst()
                .orElseThrow();
        int initialStock = mouse.getStockQuantity();

        // When - Reduce stock (simulate purchase)
        productService.updateStock(mouse.getId(), -5);

        // Then
        Product updatedProduct = productRepository.findById(mouse.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(initialStock - 5);

        // When - Increase stock (simulate restock)
        productService.updateStock(mouse.getId(), 20);

        // Then
        updatedProduct = productRepository.findById(mouse.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(initialStock - 5 + 20);
    }

    @Test
    void searchProducts_shouldHandleSpecialCharacters() {
        // Given
        createProduct("C++ Programming", "Learn C++ coding", new BigDecimal("44.99"), 30, books, true);

        // When
        Page<Product> results = productService.searchProducts("C++", PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).contains("C++");
    }

    @Test
    void findByCategory_shouldSupportPagination() {
        // When
        Page<Product> page1 = productService.findByCategory(electronics.getId(), PageRequest.of(0, 2));
        Page<Product> page2 = productService.findByCategory(electronics.getId(), PageRequest.of(1, 2));

        // Then
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page2.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(5);
        assertThat(page1.getTotalPages()).isEqualTo(3);
    }

    @Test
    void searchProducts_shouldMatchAcrossMultipleFields() {
        // Given - Product with "charging" in description
        // When
        Page<Product> results = productService.searchProducts("charging", PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getName()).contains("USB-C");
    }

    @Test
    void findAllProducts_shouldOrderByCreatedDate() {
        // When
        Page<Product> results = productService.findAllProducts(PageRequest.of(0, 20));

        // Then
        assertThat(results.getContent()).isNotEmpty();
        // All should be active products
        assertThat(results.getContent()).allMatch(Product::isActive);
    }

    @Test
    void searchProducts_shouldFindByPriceRange() {
        // Given - Looking for expensive items (over $100)
        Page<Product> allProducts = productService.findAllProducts(PageRequest.of(0, 20));

        // Then
        long expensiveCount = allProducts.getContent().stream()
                .filter(p -> p.getPrice().compareTo(new BigDecimal("100")) > 0)
                .count();

        assertThat(expensiveCount).isGreaterThan(0);
    }
}
