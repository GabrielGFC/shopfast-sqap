package com.shopfast.payment;

/**
 * Contrato do gateway de pagamento externo.
 * Isolado via interface para que CouponValidationService não dependa
 * diretamente de implementações de infraestrutura (Stripe, PagSeguro, etc.).
 *
 * Clean Code: dependency inversion — domínio depende de abstração, não de detalhe.
 */
public interface PaymentGateway {
    /**
     * Retorna o saldo disponível no meio de pagamento do cliente.
     * Nunca retorna null — usa CardBalance.of(BigDecimal.ZERO) para saldo zerado.
     *
     * @param customerId identificador do cliente
     * @return saldo disponível (nunca null)
     */
    CardBalance getAvailableBalance(String customerId);
}
