package io.cb_demos.ecommerce.domain;

import java.math.BigDecimal;

public enum ShippingMethod {
    STANDARD("Standard Shipping", new BigDecimal("5.99"), 5, 7),
    EXPRESS("Express Shipping", new BigDecimal("12.99"), 2, 3),
    OVERNIGHT("Overnight Shipping", new BigDecimal("24.99"), 1, 1),
    FREE("Free Shipping", BigDecimal.ZERO, 7, 10);

    private final String displayName;
    private final BigDecimal baseCost;
    private final int minDeliveryDays;
    private final int maxDeliveryDays;

    ShippingMethod(String displayName, BigDecimal baseCost, int minDeliveryDays, int maxDeliveryDays) {
        this.displayName = displayName;
        this.baseCost = baseCost;
        this.minDeliveryDays = minDeliveryDays;
        this.maxDeliveryDays = maxDeliveryDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public int getMinDeliveryDays() {
        return minDeliveryDays;
    }

    public int getMaxDeliveryDays() {
        return maxDeliveryDays;
    }

    public String getEstimatedDelivery() {
        if (minDeliveryDays == maxDeliveryDays) {
            return minDeliveryDays + " day" + (minDeliveryDays > 1 ? "s" : "");
        }
        return minDeliveryDays + "-" + maxDeliveryDays + " days";
    }
}
