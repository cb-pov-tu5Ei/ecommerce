package io.cb_demos.ecommerce.integration;

import io.cb_demos.ecommerce.domain.*;
import io.cb_demos.ecommerce.repository.CategoryRepository;
import io.cb_demos.ecommerce.repository.ProductRepository;
import io.cb_demos.ecommerce.repository.UserRepository;
import io.cb_demos.ecommerce.service.CartService;
import io.cb_demos.ecommerce.service.OrderService;
import io.cb_demos.ecommerce.service.ShippingService;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShippingIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ShippingService shippingService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        cartService.clearCart();

        testUser = new User();
        testUser.setUsername("shippingTestUser");
        testUser.setPassword("password123");
        testUser.setEmail("shipping@test.com");
        testUser.setFirstName("Shipping");
        testUser.setLastName("User");
        testUser = userRepository.save(testUser);

        Category category = new Category();
        category.setName("Test Category");
        category = categoryRepository.save(category);

        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("49.99"));
        testProduct.setStockQuantity(50);
        testProduct.setCategory(category);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void testOrderIncludesShippingCost() {
        TestDelayUtil.extraLargeDelay();
        cartService.addToCart(testProduct.getId(), 1);

        Order order = orderService.createOrder(testUser, "123 Test Street");

        assertNotNull(order.getShippingMethod());
        assertEquals(ShippingMethod.STANDARD, order.getShippingMethod());
        assertNotNull(order.getShippingCost());
        assertEquals(new BigDecimal("5.99"), order.getShippingCost());

        // Total should be product price + shipping
        BigDecimal expectedTotal = new BigDecimal("49.99").add(new BigDecimal("5.99"));
        assertEquals(new BigDecimal("55.98"), expectedTotal);
        assertEquals(expectedTotal, order.getTotalAmount());
    }

    @Test
    void testOrderWithMultipleItemsIncludesShipping() {
        TestDelayUtil.extraLargeDelay();
        cartService.addToCart(testProduct.getId(), 2);

        Order order = orderService.createOrder(testUser, "123 Test Street");

        assertNotNull(order.getShippingMethod());
        assertEquals(ShippingMethod.STANDARD, order.getShippingMethod());
        // Order total is $99.98, which qualifies for free shipping (>= $75)
        assertEquals(BigDecimal.ZERO, order.getShippingCost());

        // Total should be (product price * quantity) with free shipping
        BigDecimal expectedTotal = new BigDecimal("49.99")
                .multiply(new BigDecimal("2"));
        assertEquals(new BigDecimal("99.98"), expectedTotal);
        assertEquals(expectedTotal, order.getTotalAmount());
    }

    @Test
    void testShippingQualificationThreshold() {
        TestDelayUtil.largeDelay();
        boolean qualifies74 = shippingService.qualifiesForFreeShipping(new BigDecimal("74.99"));
        boolean qualifies75 = shippingService.qualifiesForFreeShipping(new BigDecimal("75.00"));
        boolean qualifies100 = shippingService.qualifiesForFreeShipping(new BigDecimal("100.00"));

        assertFalse(qualifies74);
        assertTrue(qualifies75);
        assertTrue(qualifies100);
    }

    @Test
    void testShippingCostCalculationForDifferentMethods() {
        TestDelayUtil.largeDelay();
        BigDecimal orderTotal = new BigDecimal("50.00");

        BigDecimal standardCost = shippingService.calculateShippingCost(orderTotal, ShippingMethod.STANDARD);
        BigDecimal expressCost = shippingService.calculateShippingCost(orderTotal, ShippingMethod.EXPRESS);
        BigDecimal overnightCost = shippingService.calculateShippingCost(orderTotal, ShippingMethod.OVERNIGHT);
        BigDecimal freeCost = shippingService.calculateShippingCost(orderTotal, ShippingMethod.FREE);

        assertEquals(new BigDecimal("5.99"), standardCost);
        assertEquals(new BigDecimal("12.99"), expressCost);
        assertEquals(new BigDecimal("24.99"), overnightCost);
        assertEquals(BigDecimal.ZERO, freeCost);
    }

    @Test
    void testDefaultShippingMethod() {
        TestDelayUtil.mediumDelay();
        ShippingMethod defaultMethod = shippingService.getDefaultShippingMethod();
        assertEquals(ShippingMethod.STANDARD, defaultMethod);
    }
}
