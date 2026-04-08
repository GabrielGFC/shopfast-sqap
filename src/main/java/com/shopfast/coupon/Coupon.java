package com.shopfast.coupon;

import java.math.BigDecimal;

/**
 * Entidade de domínio Coupon — imutável, sem lógica de UI.
 * Construída via Builder para expressividade e legibilidade (Clean Code).
 */
public final class Coupon {

    private final String     code;
    private final BigDecimal discountRate;
    private final boolean    expired;

    private Coupon(Builder builder) {
        this.code         = builder.code;
        this.discountRate = builder.discountRate;
        this.expired      = builder.expired;
    }

    public String     getCode()         { return code; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public boolean    isExpired()       { return expired; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String     code;
        private BigDecimal discountRate;
        private boolean    expired;

        public Builder code(String code)                  { this.code = code; return this; }
        public Builder discountRate(BigDecimal rate)      { this.discountRate = rate; return this; }
        public Builder expired(boolean expired)           { this.expired = expired; return this; }
        public Coupon  build()                            { return new Coupon(this); }
    }
}
