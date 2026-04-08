package com.shopfast.arch;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit — REGRA QG-02 (IEEE 730-2014 §4.3.1)
 *
 * Garante que nenhuma classe nos domínios de pagamento e cupom
 * contenha dependências para camadas de UI ou View.
 *
 * Este foi o vetor do incidente Black Friday: a lógica de exibição
 * da "barra verde" estava acoplada ao validador financeiro, tornando
 * impossível testar o caminho de saldo insuficiente de forma isolada.
 */
@AnalyzeClasses(packages = "com.shopfast")
class ArchitectureRulesTest {

    /**
     * REGRA QG-02 — Domínio de pagamento não pode depender de camada de UI.
     * Falha de build se violada: merge BLOQUEADO.
     */
    @ArchTest
    static final ArchRule payment_domain_must_not_depend_on_ui =
        noClasses()
            .that().resideInAnyPackage(
                "com.shopfast.payment..",
                "com.shopfast.coupon..",
                "com.shopfast.checkout.."
            )
            .should().dependOnClassesThat().resideInAnyPackage(
                "com.shopfast.view..",
                "com.shopfast.ui..",
                "com.shopfast.controller.."
            )
            .because(
                "QG-02 (IEEE 730 §4.3.1): Regras de negócio financeiras devem ser " +
                "completamente isoladas da camada de apresentação. " +
                "Violação desta regra foi a causa-raiz do incidente Black Friday."
            );

    /**
     * Domínio de pagamento não pode depender de infraestrutura diretamente.
     * Toda comunicação com serviços externos deve ocorrer via interface (abstração).
     */
    @ArchTest
    static final ArchRule payment_domain_must_use_interfaces_for_external =
        noClasses()
            .that().resideInAPackage("com.shopfast.coupon..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "com.shopfast.infra..",
                "com.shopfast.persistence.."
            )
            .because(
                "QG-02: O domínio de cupom deve depender apenas de abstrações (interfaces), " +
                "nunca de implementações de infraestrutura diretamente."
            );
}
