package io.cb_demos.ecommerce.repository;

import io.cb_demos.ecommerce.domain.Category;
import io.cb_demos.ecommerce.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;
    private Category books;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        electronics = new Category();
        electronics.setName("Electronics");
        electronics.setDescription("Electronic products");
        electronics = categoryRepository.save(electronics);

        books = new Category();
        books.setName("Books");
        books.setDescription("Physical books");
        books = categoryRepository.save(books);
    }

    @Test
    void findByActiveTrue_shouldReturnOnlyActiveProducts() {
        // Given
        Product active1 = createProduct("Active 1", new BigDecimal("10.00"), 5, electronics, true);
        Product active2 = createProduct("Active 2", new BigDecimal("20.00"), 10, electronics, true);
        Product inactive = createProduct("Inactive", new BigDecimal("30.00"), 0, electronics, false);

        // When
        Page<Product> results = productRepository.findByActiveTrue(PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(2);
        assertThat(results.getContent()).contains(active1, active2);
        assertThat(results.getContent()).doesNotContain(inactive);
    }

    @Test
    void findByCategoryAndActiveTrue_shouldFilterByCategory() {
        // Given
        Product elec1 = createProduct("Laptop", new BigDecimal("999.99"), 5, electronics, true);
        Product elec2 = createProduct("Mouse", new BigDecimal("29.99"), 10, electronics, true);
        Product book1 = createProduct("Java Book", new BigDecimal("39.99"), 8, books, true);

        // When
        Page<Product> electronicsProducts = productRepository.findByCategoryAndActiveTrue(
                electronics, PageRequest.of(0, 10));

        // Then
        assertThat(electronicsProducts.getContent()).hasSize(2);
        assertThat(electronicsProducts.getContent()).contains(elec1, elec2);
        assertThat(electronicsProducts.getContent()).doesNotContain(book1);
    }

    @Test
    void searchProducts_shouldFindByName() {
        // Given
        Product laptop = createProduct("Gaming Laptop", new BigDecimal("1299.99"), 3, electronics, true);
        Product mouse = createProduct("Wireless Mouse", new BigDecimal("29.99"), 10, electronics, true);

        // When
        Page<Product> results = productRepository.searchProducts("Laptop", PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0)).isEqualTo(laptop);
    }

    @Test
    void searchProducts_shouldFindByDescription() {
        // Given
        Product product = createProduct("USB Cable", new BigDecimal("9.99"), 50, electronics, true);
        product.setDescription("Fast charging cable");
        productRepository.save(product);

        // When
        Page<Product> results = productRepository.searchProducts("charging", PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0)).isEqualTo(product);
    }

    @Test
    void searchProducts_shouldBeCaseInsensitive() {
        // Given
        createProduct("Laptop Pro", new BigDecimal("1499.99"), 2, electronics, true);

        // When
        Page<Product> upper = productRepository.searchProducts("LAPTOP", PageRequest.of(0, 10));
        Page<Product> lower = productRepository.searchProducts("laptop", PageRequest.of(0, 10));
        Page<Product> mixed = productRepository.searchProducts("LaPtOp", PageRequest.of(0, 10));

        // Then
        assertThat(upper.getContent()).hasSize(1);
        assertThat(lower.getContent()).hasSize(1);
        assertThat(mixed.getContent()).hasSize(1);
    }

    @Test
    void findTop8ByActiveTrueOrderByCreatedAtDesc_shouldReturnLatestProducts() {
        // Given - Create 10 products
        for (int i = 1; i <= 10; i++) {
            createProduct("Product " + i, new BigDecimal("10.00"), 5, electronics, true);
        }

        // When
        List<Product> latest = productRepository.findTop8ByActiveTrueOrderByCreatedAtDesc();

        // Then
        assertThat(latest).hasSize(8);
    }

    @Test
    void findTop8ByActiveTrueOrderByCreatedAtDesc_shouldExcludeInactive() {
        // Given
        for (int i = 1; i <= 5; i++) {
            createProduct("Active " + i, new BigDecimal("10.00"), 5, electronics, true);
        }
        for (int i = 1; i <= 5; i++) {
            createProduct("Inactive " + i, new BigDecimal("10.00"), 0, electronics, false);
        }

        // When
        List<Product> latest = productRepository.findTop8ByActiveTrueOrderByCreatedAtDesc();

        // Then
        assertThat(latest).hasSize(5);
        assertThat(latest).allMatch(Product::isActive);
    }

    @Test
    void save_shouldPersistProduct() {
        // Given
        Product product = new Product();
        product.setName("New Product");
        product.setDescription("Test description");
        product.setPrice(new BigDecimal("49.99"));
        product.setStockQuantity(10);
        product.setCategory(electronics);
        product.setActive(true);

        // When
        Product saved = productRepository.save(product);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(productRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void delete_shouldRemoveProduct() {
        // Given
        Product product = createProduct("To Delete", new BigDecimal("10.00"), 5, electronics, true);
        Long productId = product.getId();

        // When
        productRepository.delete(product);

        // Then
        assertThat(productRepository.findById(productId)).isEmpty();
    }

    @Test
    void findByCategoryAndActiveTrue_shouldSupportPagination() {
        // Given - Create 15 electronics products
        for (int i = 1; i <= 15; i++) {
            createProduct("Product " + i, new BigDecimal("10.00"), 5, electronics, true);
        }

        // When
        Page<Product> page1 = productRepository.findByCategoryAndActiveTrue(
                electronics, PageRequest.of(0, 5));
        Page<Product> page2 = productRepository.findByCategoryAndActiveTrue(
                electronics, PageRequest.of(1, 5));

        // Then
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page2.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isEqualTo(15);
        assertThat(page1.getTotalPages()).isEqualTo(3);
    }

    @Test
    void searchProducts_shouldReturnEmptyForNoMatch() {
        // Given
        createProduct("Laptop", new BigDecimal("999.99"), 5, electronics, true);

        // When
        Page<Product> results = productRepository.searchProducts("NonExistent", PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllProducts() {
        // Given
        createProduct("Product 1", new BigDecimal("10.00"), 5, electronics, true);
        createProduct("Product 2", new BigDecimal("20.00"), 10, electronics, false);
        createProduct("Product 3", new BigDecimal("30.00"), 15, books, true);

        // When
        List<Product> all = productRepository.findAll();

        // Then
        assertThat(all).hasSize(3);
    }

    @Test
    void searchProducts_shouldHandlePartialMatches() {
        // Given
        createProduct("Wireless Keyboard", new BigDecimal("79.99"), 10, electronics, true);
        createProduct("Mechanical Keyboard", new BigDecimal("129.99"), 5, electronics, true);
        createProduct("USB Cable", new BigDecimal("9.99"), 50, electronics, true);

        // When
        Page<Product> results = productRepository.searchProducts("Keyboard", PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).hasSize(2);
    }

    @Test
    void findByCategory_shouldReturnEmptyForCategoryWithNoProducts() {
        // Given - books category has no products
        createProduct("Laptop", new BigDecimal("999.99"), 5, electronics, true);

        // When
        Page<Product> results = productRepository.findByCategoryAndActiveTrue(
                books, PageRequest.of(0, 10));

        // Then
        assertThat(results.getContent()).isEmpty();
    }

    private Product createProduct(String name, BigDecimal price, int stock, Category category, boolean active) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Description for " + name);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setCategory(category);
        product.setActive(active);
        return productRepository.save(product);
    }
}
