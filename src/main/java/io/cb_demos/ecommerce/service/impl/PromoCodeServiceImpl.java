package io.cb_demos.ecommerce.service.impl;

import io.cb_demos.ecommerce.domain.PromoCode;
import io.cb_demos.ecommerce.repository.PromoCodeRepository;
import io.cb_demos.ecommerce.service.PromoCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromoCodeServiceImpl implements PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<PromoCode> findByCode(String code) {
        return promoCodeRepository.findByCode(code.toUpperCase());
    }

    @Override
    @Transactional
    public PromoCode createPromoCode(String code, BigDecimal discountPercentage) {
        PromoCode promoCode = new PromoCode();
        promoCode.setCode(code.toUpperCase());
        promoCode.setDiscountPercentage(discountPercentage);
        promoCode.setActive(true);
        return promoCodeRepository.save(promoCode);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validatePromoCode(String code) {
        Optional<PromoCode> promoCode = findByCode(code);
        return promoCode.isPresent() && promoCode.get().isValid();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(BigDecimal subtotal, String promoCode) {
        Optional<PromoCode> promo = findByCode(promoCode);
        if (promo.isEmpty() || !promo.get().isValid()) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountPercentage = promo.get().getDiscountPercentage();
        return subtotal.multiply(discountPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
