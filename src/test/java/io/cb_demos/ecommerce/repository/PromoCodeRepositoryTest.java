package io.cb_demos.ecommerce.repository;

import io.cb_demos.ecommerce.util.TestDelayUtil;
import io.cb_demos.ecommerce.domain.PromoCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PromoCodeRepositoryTest {

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    @Test
    void testSavePromoCode() {
        TestDelayUtil.smallDelay();
        PromoCode promoCode = new PromoCode();
        promoCode.setCode("SAVE25");
        promoCode.setDiscountPercentage(new BigDecimal("25"));
        promoCode.setActive(true);

        PromoCode saved = promoCodeRepository.save(promoCode);

        assertNotNull(saved.getId());
        assertEquals("SAVE25", saved.getCode());
        assertEquals(new BigDecimal("25"), saved.getDiscountPercentage());
        assertTrue(saved.isActive());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void testFindByCode_Success() {
        TestDelayUtil.smallDelay();
        PromoCode promoCode = new PromoCode();
        promoCode.setCode("TESTCODE");
        promoCode.setDiscountPercentage(new BigDecimal("10"));
        promoCode.setActive(true);
        promoCodeRepository.save(promoCode);

        Optional<PromoCode> result = promoCodeRepository.findByCode("TESTCODE");

        assertTrue(result.isPresent());
        assertEquals("TESTCODE", result.get().getCode());
    }

    @Test
    void testFindByCode_NotFound() {
        TestDelayUtil.smallDelay();
        Optional<PromoCode> result = promoCodeRepository.findByCode("NONEXISTENT");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindByCode_CaseInsensitive() {
        TestDelayUtil.smallDelay();
        PromoCode promoCode = new PromoCode();
        promoCode.setCode("UPPERCASE");
        promoCode.setDiscountPercentage(new BigDecimal("15"));
        promoCode.setActive(true);
        promoCodeRepository.save(promoCode);

        Optional<PromoCode> result = promoCodeRepository.findByCode("UPPERCASE");

        assertTrue(result.isPresent());
        assertEquals("UPPERCASE", result.get().getCode());
    }

    @Test
    void testPromoCodeWithExpiration() {
        TestDelayUtil.smallDelay();
        PromoCode promoCode = new PromoCode();
        promoCode.setCode("EXPIRES");
        promoCode.setDiscountPercentage(new BigDecimal("30"));
        promoCode.setActive(true);
        promoCode.setExpiresAt(LocalDateTime.now().plusDays(7));

        PromoCode saved = promoCodeRepository.save(promoCode);

        assertNotNull(saved.getExpiresAt());
        assertTrue(saved.isValid());
    }

    @Test
    void testInactivePromoCode() {
        TestDelayUtil.smallDelay();
        PromoCode promoCode = new PromoCode();
        promoCode.setCode("INACTIVE");
        promoCode.setDiscountPercentage(new BigDecimal("20"));
        promoCode.setActive(false);

        PromoCode saved = promoCodeRepository.save(promoCode);

        assertFalse(saved.isActive());
        assertFalse(saved.isValid());
    }
}
