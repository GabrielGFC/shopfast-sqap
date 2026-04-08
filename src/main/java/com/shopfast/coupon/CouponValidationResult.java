package com.shopfast.coupon;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Resultado imutável da validação de cupom.
 * Elimina retornos booleanos ambíguos e garante que o chamador sempre
 * saiba o motivo de uma rejeição (Clean Code: evite flags implícitas).
 */
public final class CouponValidationResult {

    private final boolean    valid;
    private final String     rejectionReason;
    private final BigDecimal discountedAmount;
    private final BigDecimal discountRate;

    private CouponValidationResult(
            boolean    valid,
            String     rejectionReason,
            BigDecimal discountedAmount,
            BigDecimal discountRate) {
        this.valid            = valid;
        this.rejectionReason  = rejectionReason;
        this.discountedAmount = discountedAmount;
        this.discountRate     = discountRate;
    }

    public static CouponValidationResult valid(BigDecimal discountedAmount, BigDecimal discountRate) {
        Objects.requireNonNull(discountedAmount, "discountedAmount must not be null");
        Objects.requireNonNull(discountRate,     "discountRate must not be null");
        return new CouponValidationResult(true, null, discountedAmount, discountRate);
    }

    public static CouponValidationResult invalid(String reason) {
        Objects.requireNonNull(reason, "rejection reason must not be null");
        return new CouponValidationResult(false, reason, null, null);
    }

    public boolean isValid()                  { return valid; }
    public String  getRejectionReason()       { return rejectionReason; }
    public BigDecimal getDiscountedAmount()   { return discountedAmount; }
    public BigDecimal getDiscountRate()       { return discountRate; }
}
