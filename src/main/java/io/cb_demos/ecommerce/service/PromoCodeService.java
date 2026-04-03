package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.domain.PromoCode;

import java.math.BigDecimal;
import java.util.Optional;

public interface PromoCodeService {
    Optional<PromoCode> findByCode(String code);
    PromoCode createPromoCode(String code, BigDecimal discountPercentage);
    boolean validatePromoCode(String code);
    BigDecimal calculateDiscount(BigDecimal subtotal, String promoCode);
}
