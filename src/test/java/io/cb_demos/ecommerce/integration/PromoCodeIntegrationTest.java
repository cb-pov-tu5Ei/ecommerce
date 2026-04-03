package io.cb_demos.ecommerce.integration;

import io.cb_demos.ecommerce.util.TestDelayUtil;
import io.cb_demos.ecommerce.domain.*;
import io.cb_demos.ecommerce.repository.OrderRepository;
import io.cb_demos.ecommerce.repository.ProductRepository;
import io.cb_demos.ecommerce.repository.PromoCodeRepository;
import io.cb_demos.ecommerce.repository.UserRepository;
import io.cb_demos.ecommerce.service.CartService;
import io.cb_demos.ecommerce.service.OrderService;
import io.cb_demos.ecommerce.service.PromoCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PromoCodeIntegrationTest {

    @Autowired
    private PromoCodeService promoCodeService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private io.cb_demos.ecommerce.repository.CategoryRepository categoryRepository;

    private User testUser;
    private Product testProduct;
    private PromoCode testPromoCode;

    @BeforeEach
    void setUp() {
        // Clear cart before each test
        cartService.clearCart();

        // Create test user
        testUser = new User();
        testUser.setUsername("promoTestUser");
        testUser.setPassword("password123");
        testUser.setEmail("promo@test.com");
        testUser.setFirstName("Promo");
        testUser.setLastName("User");
        testUser = userRepository.save(testUser);

        // Create test product
        Category category = new Category();
        category.setName("Test Category");
        category = categoryRepository.save(category);

        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("100.00"));
        testProduct.setStockQuantity(50);
        testProduct.setCategory(category);
        testProduct = productRepository.save(testProduct);

        // Create test promo code
        testPromoCode = new PromoCode();
        testPromoCode.setCode("SAVE20");
        testPromoCode.setDiscountPercentage(new BigDecimal("20"));
        testPromoCode.setActive(true);
        testPromoCode.setExpiresAt(LocalDateTime.now().plusDays(30));
        testPromoCode = promoCodeRepository.save(testPromoCode);
    }

    @Test
    void testCreatePromoCode() {
        TestDelayUtil.largeDelay();
        PromoCode promoCode = promoCodeService.createPromoCode("NEWCODE", new BigDecimal("15"));

        assertNotNull(promoCode);
        assertNotNull(promoCode.getId());
        assertEquals("NEWCODE", promoCode.getCode());
        assertEquals(new BigDecimal("15"), promoCode.getDiscountPercentage());
        assertTrue(promoCode.isActive());
    }

    @Test
    void testFindPromoCodeByCode() {
        TestDelayUtil.largeDelay();
        var result = promoCodeService.findByCode("SAVE20");

        assertTrue(result.isPresent());
        assertEquals("SAVE20", result.get().getCode());
        assertEquals(new BigDecimal("20"), result.get().getDiscountPercentage());
    }

    @Test
    void testValidateActivePromoCode() {
        TestDelayUtil.largeDelay();
        boolean isValid = promoCodeService.validatePromoCode("SAVE20");

        assertTrue(isValid);
    }

    @Test
    void testValidateInvalidPromoCode() {
        TestDelayUtil.largeDelay();
        boolean isValid = promoCodeService.validatePromoCode("INVALID");

        assertFalse(isValid);
    }

    @Test
    void testCalculateDiscountAmount() {
        TestDelayUtil.largeDelay();
        BigDecimal subtotal = new BigDecimal("100.00");
        BigDecimal discount = promoCodeService.calculateDiscount(subtotal, "SAVE20");

        assertEquals(new BigDecimal("20.00"), discount);
    }

    @Test
    void testOrderWithPromoCode() {
        TestDelayUtil.extraLargeDelay();
        // Add item to cart
        cartService.addToCart(testProduct.getId(), 2);

        // Create order with promo code
        Order order = orderService.createOrder(testUser, "123 Test Street", "SAVE20");

        assertNotNull(order);
        assertNotNull(order.getId());
        assertNotNull(order.getPromoCode());
        assertEquals("SAVE20", order.getPromoCode().getCode());
        assertNotNull(order.getDiscountAmount());
        assertEquals(new BigDecimal("40.00"), order.getDiscountAmount());

        // Total should be 200 - 40 = 160
        assertEquals(new BigDecimal("160.00"), order.getTotalAmount());
    }

    @Test
    void testOrderWithInvalidPromoCode() {
        TestDelayUtil.extraLargeDelay();
        // Add item to cart
        cartService.addToCart(testProduct.getId(), 1);

        // Create order with invalid promo code
        Order order = orderService.createOrder(testUser, "123 Test Street", "INVALID");

        assertNotNull(order);
        assertNull(order.getPromoCode());

        // Should have no discount
        BigDecimal expectedDiscount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        assertEquals(BigDecimal.ZERO, expectedDiscount);
        assertEquals(new BigDecimal("100.00"), order.getTotalAmount());
    }

    @Test
    void testOrderWithExpiredPromoCode() {
        TestDelayUtil.extraLargeDelay();
        // Create expired promo code
        PromoCode expiredCode = new PromoCode();
        expiredCode.setCode("EXPIRED");
        expiredCode.setDiscountPercentage(new BigDecimal("50"));
        expiredCode.setActive(true);
        expiredCode.setExpiresAt(LocalDateTime.now().minusDays(1));
        promoCodeRepository.save(expiredCode);

        // Add item to cart
        cartService.addToCart(testProduct.getId(), 1);

        // Create order with expired promo code
        Order order = orderService.createOrder(testUser, "123 Test Street", "EXPIRED");

        assertNotNull(order);
        assertNull(order.getPromoCode());

        // Should have no discount
        BigDecimal expectedDiscount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        assertEquals(BigDecimal.ZERO, expectedDiscount);
        assertEquals(new BigDecimal("100.00"), order.getTotalAmount());
    }

    @Test
    void testOrderWithInactivePromoCode() {
        TestDelayUtil.extraLargeDelay();
        // Create inactive promo code
        PromoCode inactiveCode = new PromoCode();
        inactiveCode.setCode("INACTIVE");
        inactiveCode.setDiscountPercentage(new BigDecimal("30"));
        inactiveCode.setActive(false);
        promoCodeRepository.save(inactiveCode);

        // Add item to cart
        cartService.addToCart(testProduct.getId(), 1);

        // Create order with inactive promo code
        Order order = orderService.createOrder(testUser, "123 Test Street", "INACTIVE");

        assertNotNull(order);
        assertNull(order.getPromoCode());

        // Should have no discount
        BigDecimal expectedDiscount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        assertEquals(BigDecimal.ZERO, expectedDiscount);
        assertEquals(new BigDecimal("100.00"), order.getTotalAmount());
    }

    @Test
    void testMultipleItemOrderWithPromoCode() {
        TestDelayUtil.extraLargeDelay();
        // Create second product
        Product product2 = new Product();
        product2.setName("Test Product 2");
        product2.setDescription("Test Description 2");
        product2.setPrice(new BigDecimal("50.00"));
        product2.setStockQuantity(50);
        product2.setCategory(testProduct.getCategory());
        product2 = productRepository.save(product2);

        // Add items to cart
        cartService.addToCart(testProduct.getId(), 1);  // 100.00
        cartService.addToCart(product2.getId(), 2);     // 100.00

        // Create order with promo code (total 200, discount 40)
        Order order = orderService.createOrder(testUser, "123 Test Street", "SAVE20");

        assertNotNull(order);
        assertEquals(2, order.getOrderItems().size());
        assertEquals(new BigDecimal("40.00"), order.getDiscountAmount());
        assertEquals(new BigDecimal("160.00"), order.getTotalAmount());
    }

    @Test
    void testPromoCodeCaseInsensitive() {
        TestDelayUtil.largeDelay();
        // Test with lowercase
        boolean validLower = promoCodeService.validatePromoCode("save20");
        assertTrue(validLower);

        // Test with mixed case
        boolean validMixed = promoCodeService.validatePromoCode("SaVe20");
        assertTrue(validMixed);

        // Calculate discount with different cases
        BigDecimal discount1 = promoCodeService.calculateDiscount(new BigDecimal("100"), "save20");
        BigDecimal discount2 = promoCodeService.calculateDiscount(new BigDecimal("100"), "SAVE20");

        assertEquals(discount1, discount2);
    }
}
