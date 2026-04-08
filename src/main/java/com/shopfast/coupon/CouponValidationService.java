package com.shopfast.coupon;

import com.shopfast.payment.CardBalance;
import com.shopfast.payment.PaymentGateway;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * CouponValidationService — Validador de cupons promocionais da ShopFast.
 *
 * <p>RESPONSABILIDADE ÚNICA (SRP): Esta classe valida exclusivamente a elegibilidade
 * de aplicação de um cupom sobre um pedido. Não aciona despacho logístico,
 * não manipula camadas de UI nem registra eventos de auditoria — cada
 * responsabilidade pertence ao seu próprio domínio.</p>
 *
 * <p>CONTEXTO (Incidente Black Friday): O código predecessssor combinava validação
 * de cupom, exibição de UI e despacho logístico em um único método, tornando
 * impossível testar o caminho de saldo insuficiente de forma isolada.
 * Esta refatoração corrige a violação de SRP e torna cada regra de negócio
 * individualmente verificável.</p>
 *
 * <p>Mitigação de Risco: R1, R2 (ver README.md §4 — Matriz P×I)</p>
 *
 * @author Gabriel Fernandes de Carvalho — Matrícula 2320142
 * @version 2.1.0
 * @see com.shopfast.payment.PaymentGateway
 */
public class CouponValidationService {

    private static final BigDecimal MINIMUM_ORDER_AMOUNT = new BigDecimal("1.00");

    private final CouponRepository couponRepository;
    private final PaymentGateway   paymentGateway;

    public CouponValidationService(
            CouponRepository couponRepository,
            PaymentGateway   paymentGateway) {
        this.couponRepository = couponRepository;
        this.paymentGateway   = paymentGateway;
    }

    // ─── API Pública ──────────────────────────────────────────────────────────

    /**
     * Valida se um cupom pode ser aplicado a um pedido, considerando:
     * (1) existência e vigência do cupom,
     * (2) saldo disponível no meio de pagamento do cliente,
     * (3) valor mínimo do pedido após desconto.
     *
     * <p>Este método é puro em relação a efeitos colaterais: não modifica
     * estado externo, não dispara eventos de despacho nem altera a UI.</p>
     *
     * @param couponCode  código do cupom informado pelo cliente (ex: "BLACK50")
     * @param orderAmount valor bruto do pedido antes do desconto
     * @param customerId  identificador do cliente para consulta de saldo
     * @return {@link CouponValidationResult} contendo o status e, se válido,
     *         o valor do desconto calculado
     */
    public CouponValidationResult validate(
            String     couponCode,
            BigDecimal orderAmount,
            String     customerId) {

        Optional<Coupon> couponOpt = couponRepository.findActiveCoupon(couponCode);
        if (couponOpt.isEmpty()) {
            return CouponValidationResult.invalid("COUPON_NOT_FOUND");
        }

        Coupon coupon = couponOpt.get();

        if (coupon.isExpired()) {
            return CouponValidationResult.invalid("COUPON_EXPIRED");
        }

        BigDecimal discountedAmount = calculateDiscountedAmount(orderAmount, coupon);

        if (isAmountBelowMinimum(discountedAmount)) {
            return CouponValidationResult.invalid("ORDER_BELOW_MINIMUM_AFTER_DISCOUNT");
        }

        // ─── PONTO CRÍTICO: validação de saldo ANTES de qualquer despacho ────
        // Esta verificação estava ausente no código da Black Friday.
        // O saldo é consultado aqui, no domínio de validação, não no domínio
        // de despacho logístico.
        CardBalance balance = paymentGateway.getAvailableBalance(customerId);
        if (isBalanceInsufficient(balance, discountedAmount)) {
            return CouponValidationResult.invalid("INSUFFICIENT_CARD_BALANCE");
        }

        return CouponValidationResult.valid(discountedAmount, coupon.getDiscountRate());
    }

    // ─── Métodos Privados — Regras de Negócio Isoladas ───────────────────────

    /**
     * Calcula o valor com desconto aplicado.
     * Isolado para permitir teste unitário da regra de cálculo independentemente
     * das chamadas externas (repositório, gateway).
     */
    private BigDecimal calculateDiscountedAmount(BigDecimal orderAmount, Coupon coupon) {
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(coupon.getDiscountRate());
        return orderAmount.multiply(discountMultiplier);
    }

    private boolean isAmountBelowMinimum(BigDecimal amount) {
        return amount.compareTo(MINIMUM_ORDER_AMOUNT) < 0;
    }

    private boolean isBalanceInsufficient(CardBalance balance, BigDecimal requiredAmount) {
        return balance.getAvailable().compareTo(requiredAmount) < 0;
    }
}
