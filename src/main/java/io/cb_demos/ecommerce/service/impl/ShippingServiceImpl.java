package io.cb_demos.ecommerce.service.impl;

import io.cb_demos.ecommerce.domain.ShippingMethod;
import io.cb_demos.ecommerce.service.ShippingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ShippingServiceImpl implements ShippingService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("75.00");

    @Override
    public BigDecimal calculateShippingCost(BigDecimal orderSubtotal, ShippingMethod method) {
        if (method == null) {
            method = getDefaultShippingMethod();
        }

        // Apply free shipping for orders at or above threshold
        if (qualifiesForFreeShipping(orderSubtotal)) {
            return BigDecimal.ZERO;
        }

        return method.getBaseCost();
    }

    @Override
    public ShippingMethod getDefaultShippingMethod() {
        return ShippingMethod.STANDARD;
    }

    @Override
    public boolean qualifiesForFreeShipping(BigDecimal orderSubtotal) {
        return orderSubtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0;
    }
}
