package com.shopfast.payment;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object que representa o saldo disponível em um meio de pagamento.
 * Imutável — garante que nenhum validador altere o saldo como efeito colateral.
 */
public final class CardBalance {

    private final BigDecimal available;

    private CardBalance(BigDecimal available) {
        this.available = Objects.requireNonNull(available, "available balance must not be null");
    }

    public static CardBalance of(BigDecimal available) {
        return new CardBalance(available);
    }

    public BigDecimal getAvailable() { return available; }
}
