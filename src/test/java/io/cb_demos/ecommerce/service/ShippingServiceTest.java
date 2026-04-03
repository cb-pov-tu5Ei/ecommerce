package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.ShippingMethod;
import io.cb_demos.ecommerce.service.impl.ShippingServiceImpl;
import io.cb_demos.ecommerce.util.TestDelayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ShippingServiceTest {

    private ShippingService shippingService;

    @BeforeEach
    void setUp() {
        shippingService = new ShippingServiceImpl();
    }

    @Test
    void testCalculateShippingCost_Standard() {
        TestDelayUtil.mediumDelay();
        BigDecimal cost = shippingService.calculateShippingCost(new BigDecimal("50.00"), ShippingMethod.STANDARD);
        assertEquals(new BigDecimal("5.99"), cost);
    }

    @Test
    void testCalculateShippingCost_Express() {
        TestDelayUtil.mediumDelay();
        BigDecimal cost = shippingService.calculateShippingCost(new BigDecimal("50.00"), ShippingMethod.EXPRESS);
        assertEquals(new BigDecimal("12.99"), cost);
    }

    @Test
    void testCalculateShippingCost_Overnight() {
        TestDelayUtil.mediumDelay();
        BigDecimal cost = shippingService.calculateShippingCost(new BigDecimal("50.00"), ShippingMethod.OVERNIGHT);
        assertEquals(new BigDecimal("24.99"), cost);
    }

    @Test
    void testCalculateShippingCost_Free() {
        TestDelayUtil.mediumDelay();
        BigDecimal cost = shippingService.calculateShippingCost(new BigDecimal("50.00"), ShippingMethod.FREE);
        assertEquals(BigDecimal.ZERO, cost);
    }

    @Test
    void testCalculateShippingCost_NullMethod_UsesDefault() {
        TestDelayUtil.mediumDelay();
        BigDecimal cost = shippingService.calculateShippingCost(new BigDecimal("50.00"), null);
        assertEquals(new BigDecimal("5.99"), cost);
    }

    @Test
    void testGetDefaultShippingMethod() {
        TestDelayUtil.smallDelay();
        ShippingMethod method = shippingService.getDefaultShippingMethod();
        assertEquals(ShippingMethod.STANDARD, method);
    }

    @Test
    void testQualifiesForFreeShipping_BelowThreshold() {
        TestDelayUtil.smallDelay();
        boolean qualifies = shippingService.qualifiesForFreeShipping(new BigDecimal("74.99"));
        assertFalse(qualifies);
    }

    @Test
    void testQualifiesForFreeShipping_AtThreshold() {
        TestDelayUtil.smallDelay();
        boolean qualifies = shippingService.qualifiesForFreeShipping(new BigDecimal("75.00"));
        assertTrue(qualifies);
    }

    @Test
    void testQualifiesForFreeShipping_AboveThreshold() {
        TestDelayUtil.smallDelay();
        boolean qualifies = shippingService.qualifiesForFreeShipping(new BigDecimal("100.00"));
        assertTrue(qualifies);
    }

    @Test
    void testShippingMethodDisplayNames() {
        TestDelayUtil.smallDelay();
        assertEquals("Standard Shipping", ShippingMethod.STANDARD.getDisplayName());
        assertEquals("Express Shipping", ShippingMethod.EXPRESS.getDisplayName());
        assertEquals("Overnight Shipping", ShippingMethod.OVERNIGHT.getDisplayName());
        assertEquals("Free Shipping", ShippingMethod.FREE.getDisplayName());
    }

    @Test
    void testShippingMethodDeliveryEstimates() {
        TestDelayUtil.smallDelay();
        assertEquals("5-7 days", ShippingMethod.STANDARD.getEstimatedDelivery());
        assertEquals("2-3 days", ShippingMethod.EXPRESS.getEstimatedDelivery());
        assertEquals("1 day", ShippingMethod.OVERNIGHT.getEstimatedDelivery());
        assertEquals("7-10 days", ShippingMethod.FREE.getEstimatedDelivery());
    }
}
