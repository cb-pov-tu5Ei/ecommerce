package io.cb_demos.ecommerce.exception;

import java.math.BigDecimal;

public class MinimumOrderValueException extends RuntimeException {

    private final BigDecimal currentTotal;
    private final BigDecimal minimumRequired;

    public MinimumOrderValueException(BigDecimal currentTotal, BigDecimal minimumRequired) {
        super(String.format("Order total of $%.2f does not meet minimum order value of $%.2f. Add $%.2f more to proceed.",
                currentTotal, minimumRequired, minimumRequired.subtract(currentTotal)));
        this.currentTotal = currentTotal;
        this.minimumRequired = minimumRequired;
    }

    public BigDecimal getCurrentTotal() {
        return currentTotal;
    }

    public BigDecimal getMinimumRequired() {
        return minimumRequired;
    }

    public BigDecimal getAmountNeeded() {
        return minimumRequired.subtract(currentTotal);
    }
}
