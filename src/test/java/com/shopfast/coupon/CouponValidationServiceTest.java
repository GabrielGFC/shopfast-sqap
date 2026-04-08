package com.shopfast.coupon;

import com.shopfast.payment.CardBalance;
import com.shopfast.payment.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes de unidade para CouponValidationService.
 *
 * Cobertura exigida pela REGRA QG-01:
 * ✅ Caminho feliz (cupom válido + saldo suficiente)
 * ✅ Cupom inexistente
 * ✅ Cupom expirado
 * ✅ Saldo insuficiente (cenário do incidente Black Friday — R1)
 * ✅ Valor do pedido abaixo do mínimo após desconto
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CouponValidationService — Testes de Regra de Negócio")
class CouponValidationServiceTest {

    @Mock private CouponRepository couponRepository;
    @Mock private PaymentGateway   paymentGateway;

    private CouponValidationService service;

    private static final String     CUSTOMER_ID   = "customer-123";
    private static final String     COUPON_CODE   = "BLACK50";
    private static final BigDecimal ORDER_AMOUNT  = new BigDecimal("2000.00");
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.50");

    @BeforeEach
    void setUp() {
        service = new CouponValidationService(couponRepository, paymentGateway);
    }

    // ─── Caminho Feliz ────────────────────────────────────────────────────────

    @Test
    @DisplayName("✅ Deve aprovar cupom quando saldo é suficiente e cupom está ativo")
    void shouldApproveCoupon_whenBalanceSufficientAndCouponActive() {
        Coupon activeCoupon = buildActiveCoupon(DISCOUNT_RATE);
        when(couponRepository.findActiveCoupon(COUPON_CODE)).thenReturn(Optional.of(activeCoupon));
        when(paymentGateway.getAvailableBalance(CUSTOMER_ID))
            .thenReturn(CardBalance.of(new BigDecimal("1000.00"))); // saldo = valor pós-desconto

        CouponValidationResult result = service.validate(COUPON_CODE, ORDER_AMOUNT, CUSTOMER_ID);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDiscountedAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.getDiscountRate()).isEqualByComparingTo("0.50");
    }

    // ─── Cenário do Incidente Black Friday (R1) ───────────────────────────────

    @Test
    @DisplayName("🚨 R1 — Deve REJEITAR cupom quando saldo do cartão é insuficiente (incidente Black Friday)")
    void shouldRejectCoupon_whenCardBalanceIsInsufficient() {
        Coupon activeCoupon = buildActiveCoupon(DISCOUNT_RATE);
        when(couponRepository.findActiveCoupon(COUPON_CODE)).thenReturn(Optional.of(activeCoupon));
        when(paymentGateway.getAvailableBalance(CUSTOMER_ID))
            .thenReturn(CardBalance.of(BigDecimal.ZERO)); // ← saldo zerado

        CouponValidationResult result = service.validate(COUPON_CODE, ORDER_AMOUNT, CUSTOMER_ID);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getRejectionReason()).isEqualTo("INSUFFICIENT_CARD_BALANCE");
    }

    // ─── Caminhos de Exceção ──────────────────────────────────────────────────

    @Test
    @DisplayName("❌ Deve rejeitar cupom inexistente no repositório")
    void shouldRejectCoupon_whenCouponNotFound() {
        when(couponRepository.findActiveCoupon(COUPON_CODE)).thenReturn(Optional.empty());

        CouponValidationResult result = service.validate(COUPON_CODE, ORDER_AMOUNT, CUSTOMER_ID);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getRejectionReason()).isEqualTo("COUPON_NOT_FOUND");
    }

    @Test
    @DisplayName("❌ Deve rejeitar cupom expirado")
    void shouldRejectCoupon_whenCouponIsExpired() {
        Coupon expiredCoupon = buildExpiredCoupon(DISCOUNT_RATE);
        when(couponRepository.findActiveCoupon(COUPON_CODE)).thenReturn(Optional.of(expiredCoupon));

        CouponValidationResult result = service.validate(COUPON_CODE, ORDER_AMOUNT, CUSTOMER_ID);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getRejectionReason()).isEqualTo("COUPON_EXPIRED");
    }

    @Test
    @DisplayName("❌ Deve rejeitar quando valor pós-desconto fica abaixo do mínimo permitido")
    void shouldRejectCoupon_whenDiscountedAmountIsBelowMinimum() {
        BigDecimal smallOrder = new BigDecimal("1.50");
        BigDecimal discount99 = new BigDecimal("0.99"); // R$ 1,50 * 0.01 = R$ 0,015 < R$ 1,00
        Coupon coupon = buildActiveCoupon(discount99);
        when(couponRepository.findActiveCoupon(COUPON_CODE)).thenReturn(Optional.of(coupon));

        CouponValidationResult result = service.validate(COUPON_CODE, smallOrder, CUSTOMER_ID);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getRejectionReason()).isEqualTo("ORDER_BELOW_MINIMUM_AFTER_DISCOUNT");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Coupon buildActiveCoupon(BigDecimal discountRate) {
        return Coupon.builder()
            .code(COUPON_CODE)
            .discountRate(discountRate)
            .expired(false)
            .build();
    }

    private Coupon buildExpiredCoupon(BigDecimal discountRate) {
        return Coupon.builder()
            .code(COUPON_CODE)
            .discountRate(discountRate)
            .expired(true)
            .build();
    }
}
