package io.cb_demos.ecommerce.service;

import io.cb_demos.ecommerce.util.TestDelayUtil;
import io.cb_demos.ecommerce.domain.PromoCode;
import io.cb_demos.ecommerce.repository.PromoCodeRepository;
import io.cb_demos.ecommerce.service.impl.PromoCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromoCodeServiceTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @InjectMocks
    private PromoCodeServiceImpl promoCodeService;

    private PromoCode activePromoCode;
    private PromoCode inactivePromoCode;
    private PromoCode expiredPromoCode;

    @BeforeEach
    void setUp() {
        activePromoCode = new PromoCode();
        activePromoCode.setId(1L);
        activePromoCode.setCode("SAVE20");
        activePromoCode.setDiscountPercentage(new BigDecimal("20"));
        activePromoCode.setActive(true);
        activePromoCode.setExpiresAt(LocalDateTime.now().plusDays(30));

        inactivePromoCode = new PromoCode();
        inactivePromoCode.setId(2L);
        inactivePromoCode.setCode("INACTIVE");
        inactivePromoCode.setDiscountPercentage(new BigDecimal("15"));
        inactivePromoCode.setActive(false);

        expiredPromoCode = new PromoCode();
        expiredPromoCode.setId(3L);
        expiredPromoCode.setCode("EXPIRED");
        expiredPromoCode.setDiscountPercentage(new BigDecimal("25"));
        expiredPromoCode.setActive(true);
        expiredPromoCode.setExpiresAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void testFindByCode_Success() {
        TestDelayUtil.mediumDelay();
        when(promoCodeRepository.findByCode("SAVE20")).thenReturn(Optional.of(activePromoCode));

        Optional<PromoCode> result = promoCodeService.findByCode("save20");

        assertTrue(result.isPresent());
        assertEquals("SAVE20", result.get().getCode());
        verify(promoCodeRepository).findByCode("SAVE20");
    }

    @Test
    void testFindByCode_NotFound() {
        TestDelayUtil.mediumDelay();
        when(promoCodeRepository.findByCode("NOTFOUND")).thenReturn(Optional.empty());

        Optional<PromoCode> result = promoCodeService.findByCode("notfound");

        assertFalse(result.isPresent());
        verify(promoCodeRepository).findByCode("NOTFOUND");
    }

    @Test
    void testCreatePromoCode_Success() {
        TestDelayUtil.mediumDelay();
        PromoCode newPromoCode = new PromoCode();
        newPromoCode.setCode("NEWCODE");
        newPromoCode.setDiscountPercentage(new BigDecimal("30"));
        newPromoCode.setActive(true);

        when(promoCodeRepository.save(any(PromoCode.class))).thenReturn(newPromoCode);

        PromoCode result = promoCodeService.createPromoCode("newcode", new BigDecimal("30"));

        assertNotNull(result);
        assertEquals("NEWCODE", result.getCode());
        assertEquals(new BigDecimal("30"), result.getDiscountPercentage());
        assertTrue(result.isActive());
        verify(promoCodeRepository).save(any(PromoCode.class));
    }

    @Test
    void testValidatePromoCode_Active() {
        TestDelayUtil.mediumDelay();
        when(promoCodeRepository.findByCode("SAVE20")).thenReturn(Optional.of(activePromoCode));

        boolean isValid = promoCodeService.validatePromoCode("save20");

        assertTrue(isValid);
        verify(promoCodeRepository).findByCode("SAVE20");
    }

    @Test
    void testValidatePromoCode_Inactive() {
        TestDelayUtil.mediumDelay();
        when(promoCodeRepository.findByCode("INACTIVE")).thenReturn(Optional.of(inactivePromoCode));

        boolean isValid = promoCodeService.validatePromoCode("inactive");

        assertFalse(isValid);
        verify(promoCodeRepository).findByCode("INACTIVE");
    }

    @Test
    void testValidatePromoCode_Expired() {
        TestDelayUtil.mediumDelay();
        when(promoCodeRepository.findByCode("EXPIRED")).thenReturn(Optional.of(expiredPromoCode));

        boolean isValid = promoCodeService.validatePromoCode("expired");

        assertFalse(isValid);
        verify(promoCodeRepository).findByCode("EXPIRED");
    }

    @Test
    void testValidatePromoCode_NotFound() {
        TestDelayUtil.mediumDelay();
        when(promoCodeRepository.findByCode("NOTFOUND")).thenReturn(Optional.empty());

        boolean isValid = promoCodeService.validatePromoCode("notfound");

        assertFalse(isValid);
        verify(promoCodeRepository).findByCode("NOTFOUND");
    }

    @Test
    void testCalculateDiscount_ValidPromoCode() {
        TestDelayUtil.mediumDelay();
        BigDecimal subtotal = new BigDecimal("100.00");
        when(promoCodeRepository.findByCode("SAVE20")).thenReturn(Optional.of(activePromoCode));

        BigDecimal discount = promoCodeService.calculateDiscount(subtotal, "save20");

        assertEquals(new BigDecimal("20.00"), discount);
        verify(promoCodeRepository).findByCode("SAVE20");
    }

    @Test
    void testCalculateDiscount_InvalidPromoCode() {
        TestDelayUtil.mediumDelay();
        BigDecimal subtotal = new BigDecimal("100.00");
        when(promoCodeRepository.findByCode("INACTIVE")).thenReturn(Optional.of(inactivePromoCode));

        BigDecimal discount = promoCodeService.calculateDiscount(subtotal, "inactive");

        assertEquals(BigDecimal.ZERO, discount);
        verify(promoCodeRepository).findByCode("INACTIVE");
    }

    @Test
    void testCalculateDiscount_PromoCodeNotFound() {
        TestDelayUtil.mediumDelay();
        BigDecimal subtotal = new BigDecimal("100.00");
        when(promoCodeRepository.findByCode("NOTFOUND")).thenReturn(Optional.empty());

        BigDecimal discount = promoCodeService.calculateDiscount(subtotal, "notfound");

        assertEquals(BigDecimal.ZERO, discount);
        verify(promoCodeRepository).findByCode("NOTFOUND");
    }

    @Test
    void testCalculateDiscount_RoundingTest() {
        TestDelayUtil.mediumDelay();
        PromoCode promoCode = new PromoCode();
        promoCode.setCode("SAVE15");
        promoCode.setDiscountPercentage(new BigDecimal("15"));
        promoCode.setActive(true);

        BigDecimal subtotal = new BigDecimal("99.99");
        when(promoCodeRepository.findByCode("SAVE15")).thenReturn(Optional.of(promoCode));

        BigDecimal discount = promoCodeService.calculateDiscount(subtotal, "save15");

        assertEquals(new BigDecimal("15.00"), discount);
        verify(promoCodeRepository).findByCode("SAVE15");
    }
}
