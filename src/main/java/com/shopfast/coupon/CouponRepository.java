package com.shopfast.coupon;

import java.util.Optional;

/**
 * Contrato de acesso ao repositório de cupons.
 * Isolado da implementação concreta (banco de dados, cache) para permitir
 * mocking nos testes de unidade — Clean Code: dependency inversion.
 */
public interface CouponRepository {
    Optional<Coupon> findActiveCoupon(String couponCode);
}
