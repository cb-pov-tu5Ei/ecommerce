package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.ShippingMethod;

import java.math.BigDecimal;

public interface ShippingService {
    BigDecimal calculateShippingCost(BigDecimal orderSubtotal, ShippingMethod method);
    ShippingMethod getDefaultShippingMethod();
    boolean qualifiesForFreeShipping(BigDecimal orderSubtotal);
}
