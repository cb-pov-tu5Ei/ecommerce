package io.cb_demos.ecommerce.init;

import io.cb_demos.ecommerce.domain.Category;
import io.cb_demos.ecommerce.domain.Product;
import io.cb_demos.ecommerce.domain.User;
import io.cb_demos.ecommerce.repository.CategoryRepository;
import io.cb_demos.ecommerce.repository.ProductRepository;
import io.cb_demos.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Loading sample data...");

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // Create categories
        Category electronics = createCategory("Electronics", "Electronic devices and gadgets");
        Category clothing = createCategory("Clothing", "Fashion and apparel");
        Category books = createCategory("Books", "Books and literature");

        // Create products
        createProduct("Laptop Pro 15", "High-performance laptop with 16GB RAM and 512GB SSD",
                new BigDecimal("1299.99"), 15, electronics,
                "https://images.unsplash.com/photo-1496181133206-80ce9b88a853");

        createProduct("Wireless Mouse", "Ergonomic wireless mouse with precision tracking",
                new BigDecimal("29.99"), 50, electronics,
                "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46");

        createProduct("Mechanical Keyboard", "RGB backlit mechanical gaming keyboard",
                new BigDecimal("89.99"), 30, electronics,
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3");

        createProduct("Smartphone X", "Latest smartphone with 6.5 inch display and 128GB storage",
                new BigDecimal("799.99"), 25, electronics,
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9");

        createProduct("Wireless Headphones", "Noise-cancelling over-ear wireless headphones",
                new BigDecimal("199.99"), 40, electronics,
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e");

        createProduct("Classic T-Shirt", "100% cotton comfortable t-shirt",
                new BigDecimal("19.99"), 100, clothing,
                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab");

        createProduct("Denim Jeans", "Classic fit denim jeans",
                new BigDecimal("49.99"), 75, clothing,
                "https://images.unsplash.com/photo-1542272604-787c3835535d");

        createProduct("Running Shoes", "Lightweight running shoes with cushioned sole",
                new BigDecimal("79.99"), 60, clothing,
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff");

        createProduct("Winter Jacket", "Warm winter jacket with insulated lining",
                new BigDecimal("129.99"), 35, clothing,
                "https://images.unsplash.com/photo-1551028719-00167b16eac5");

        createProduct("The Great Gatsby", "Classic American novel by F. Scott Fitzgerald",
                new BigDecimal("12.99"), 80, books,
                "https://images.unsplash.com/photo-1543002588-bfa74002ed7e");

        createProduct("To Kill a Mockingbird", "Pulitzer Prize winning novel by Harper Lee",
                new BigDecimal("14.99"), 65, books,
                "https://images.unsplash.com/photo-1544947950-fa07a98d237f");

        createProduct("1984", "Dystopian social science fiction by George Orwell",
                new BigDecimal("13.99"), 90, books,
                "https://images.unsplash.com/photo-1512820790803-83ca734da794");

        createProduct("Programming Book", "Learn modern web development",
                new BigDecimal("39.99"), 45, books,
                "https://images.unsplash.com/photo-1532012197267-da84d127e765");

        createProduct("Design Patterns", "Software design patterns and best practices",
                new BigDecimal("49.99"), 30, books,
                "https://images.unsplash.com/photo-1589998059171-988d887df646");

        createProduct("Clean Code", "A handbook of agile software craftsmanship",
                new BigDecimal("42.99"), 55, books,
                "https://images.unsplash.com/photo-1532012197267-da84d127e765");

        // Create admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@ecommerce.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setRole(User.UserRole.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);
        log.info("Created admin user: username=admin, password=admin123");

        // Create test user
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@ecommerce.com");
        testUser.setPassword(passwordEncoder.encode("test123"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(User.UserRole.USER);
        testUser.setEnabled(true);
        userRepository.save(testUser);
        log.info("Created test user: username=testuser, password=test123");

        log.info("Sample data loaded successfully!");
    }

    private Category createCategory(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    private Product createProduct(String name, String description, BigDecimal price,
                                   int stockQuantity, Category category, String imageUrl) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setCategory(category);
        product.setImageUrl(imageUrl);
        product.setActive(true);
        return productRepository.save(product);
    }
}
