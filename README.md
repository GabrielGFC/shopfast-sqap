# ShopFast — Mini Software Quality Assurance Plan (mini-SQAP)

> **Norma de Referência:** IEEE 730-2014 — Standard for Software Quality Assurance Processes
> **Responsável:** Gabriel Fernandes de Carvalho · Matrícula 2320142
> **Disciplina:** Qualidade de Software · UniEvangélica 2026.1
> **Repositório:** Artefato de entrega — Aula 10: Gestão da Qualidade, SQA e Riscos

---

## Sumário

1. [Visão Geral do Plano](#1-visão-geral-do-plano)
2. [Política do Quality Gate — IEEE 730 §4.4 (Enforcement)](#2-política-do-quality-gate--ieee-730-44-enforcement)
3. [Sumário Executivo de Risco — Incidente Black Friday](#3-sumário-executivo-de-risco--incidente-black-friday)
4. [Matriz de Probabilidade × Impacto](#4-matriz-de-probabilidade--impacto)
5. [Revisões e Auditorias — Peer Review Protocol](#5-revisões-e-auditorias--peer-review-protocol)
6. [Gestão de Configuração e Rastreabilidade](#6-gestão-de-configuração-e-rastreabilidade)
7. [Snippet Refatorado — Clean Code](#7-snippet-refatorado--clean-code)
8. [Referências](#8-referências)

---

## 1. Visão Geral do Plano

Este mini-SQAP documenta a **Política de Qualidade de Processo** adotada pela ShopFast após o vazamento logístico da Black Friday. Em conformidade com a **IEEE 730-2014**, o plano distingue e endereça três categorias de garantia:

| Categoria IEEE 730 | Definição | Aplicação ShopFast |
|---|---|---|
| **Garantia de Processo** (§4.3) | Assegura que o processo de desenvolvimento segue os padrões definidos | GitFlow, Definition of Done, Peer Review bloqueador |
| **Garantia de Produto** (§4.4) | Assegura que o produto gerado satisfaz os requisitos de qualidade | Cobertura de testes, análise estática, Quality Gate CI |
| **Garantia de Aplicação** (§4.5) | Assegura que o sistema integrado é apto para uso em produção | Testes de integração de pagamento, validação de regra de negócio |

> **Princípio-guia (Alan Page, Modern Testing #1):** "Nossa prioridade é acelerar o negócio — e o SQA é o mecanismo que garante que essa aceleração não gere dívida técnica letal."

---

## 2. Política do Quality Gate — IEEE 730 §4.4 (Enforcement)

Duas regras **imutáveis e automaticamente aplicadas** que, se vigentes na Black Friday, teriam bloqueado o deploy defeituoso:

---

### REGRA QG-01 — Cobertura de Teste Obrigatória em Domínios de Pagamento

```
NÍVEL: CRÍTICO | ENFORCEMENT: AUTOMÁTICO (GitHub Actions — ci-quality-gate.yml)
NORMA: IEEE 730-2014 §4.4.2 (Software Product Evaluation)
```

**Definição:** Todo Pull Request que toque nos pacotes `com.shopfast.coupon`, `com.shopfast.payment` ou `com.shopfast.checkout` **é bloqueado de merge** se qualquer uma das condições abaixo for verificada pelo pipeline de CI:

- Cobertura de linha < **90%** no pacote alterado (JaCoCo threshold);
- Ausência de teste para **caminho de exceção** de saldo insuficiente (cenário que gerou o incidente);
- Qualquer método `public` sem cobertura de teste de unidade nos domínios listados.

**Mecanismo de Bloqueio:** O job `quality-gate` no workflow `.github/workflows/ci-quality-gate.yml` falha com `exit 1` e o GitHub Branch Protection Rule `Require status checks to pass before merging` impede o merge fisicamente. Não existe override sem aprovação do Tech Lead e registro de ADR (Architecture Decision Record).

**Relação com o Incidente:** O código de Black Friday não possuía teste para `saldo_cartao = 0 AND cupom_valido = true`. Um teste de unidade cobrindo esse cenário teria falhado no pipeline antes de atingir o ambiente de staging.

---

### REGRA QG-02 — Validação de Regra de Negócio Obrigatória por Contrato de Interface

```
NÍVEL: CRÍTICO | ENFORCEMENT: AUTOMÁTICO (Checkstyle + ArchUnit — ci-quality-gate.yml)
NORMA: IEEE 730-2014 §4.3.1 (Standards, Practices, Conventions)
```

**Definição:** Nenhuma classe que implemente `PaymentProcessor` ou `CouponValidator` pode conter **lógica de apresentação ou UI acoplada** (violação de SRP). Auditado automaticamente via `ArchUnit`:

```java
// ArchUnit Rule — executada no CI antes de qualquer merge em main/develop
@ArchTest
static final ArchRule no_ui_in_payment_domain =
    noClasses()
        .that().resideInAPackage("..payment..")
        .should().dependOnClassesThat()
        .resideInAPackage("..view..").orShould()
        .dependOnClassesThat().resideInAPackage("..ui..");
```

**Relação com o Incidente:** O código da Black Friday misturou a lógica de exibição da "barra verde" com a regra de negócio do desconto. A ausência de separação de responsabilidades impediu que o validador de saldo fosse executado de forma independente e testável.

---

## 3. Sumário Executivo de Risco — Incidente Black Friday

### Descrição do Evento

Durante a campanha de Black Friday, o sistema ShopFast processou cupons promocionais (BLACK50 — 50% de desconto) **sem validar o saldo disponível no meio de pagamento do cliente**. O fluxo de aprovação visual da barra de progresso disparou o workflow de despacho logístico sem confirmação financeira, resultando no envio de unidades de alto valor (iPhones) sem contrapartida de receita. O evento classifica-se tecnicamente como **falha de Adequação Funcional** (ISO 25010 §4.2.1) combinada com **ausência de Confiabilidade de Tolerância a Falhas** (§4.4.2): o sistema não detectou nem se recuperou da condição de saldo inválido.

### Como o SQA Documentado Reduz o Change Failure Rate (DORA)

O plano de SQA atua como **barreira preventiva na origem do defeito**, não como mecanismo de detecção pós-produção. A **REGRA QG-01** obriga a existência de testes cobrindo especificamente os caminhos de exceção de pagamento antes que qualquer código alcance a branch `develop`; isso reduz diretamente o CFR (DORA Metric 3) ao eliminar a categoria de defeitos "ausência de validação de regra de negócio" ainda no ciclo de desenvolvimento. A **REGRA QG-02** garante que a arquitetura em si não permita que lógica de apresentação contamine validadores financeiros, eliminando estruturalmente a classe de bugs que gerou o incidente — uma medida de **Gestão de Risco por Prevenção/Mitigação** que atua 100 sprints antes do deploy de produção.

---

## 4. Matriz de Probabilidade × Impacto

> Metodologia: P × I com escala 1–3. Zona Vermelha (≥ 6): requer mitigação imediata no backlog da sprint corrente.

```
IMPACTO →     │ BAIXO (1)     │ MÉDIO (2)       │ ALTO (3)
──────────────┼───────────────┼─────────────────┼──────────────────────
ALTA  (3)     │       3       │       6 🟡       │  9 🔴 ← R1, R2
──────────────┼───────────────┼─────────────────┼──────────────────────
MÉDIA (2)     │       2       │       4          │  6 🟡 ← R3
──────────────┼───────────────┼─────────────────┼──────────────────────
BAIXA (1)     │       1       │       2          │  3    ← R4
```

| ID | Risco Técnico | P | I | P×I | Zona | Mitigação |
|---|---|---|---|---|---|---|
| **R1** | Cupom aprovado sem validação de saldo no gateway de pagamento | 3 | 3 | **9** | 🔴 CRÍTICO | QG-01: teste obrigatório do caminho `saldo=0` |
| **R2** | Despacho logístico acionado por evento UI antes de confirmação financeira | 3 | 3 | **9** | 🔴 CRÍTICO | QG-02: ArchUnit proíbe acoplamento UI↔Payment |
| **R3** | Latência do gateway externo causando timeout silencioso e aprovação default | 2 | 3 | **6** | 🟡 ALTO | Circuit Breaker + teste de integração com mock de timeout |
| **R4** | Dívida técnica em validadores legados (sem cobertura de teste) | 1 | 3 | **3** | 🟢 MÉDIO | Aceitar + registrar no backlog de refatoração técnica |

---

## 5. Revisões e Auditorias — Peer Review Protocol

**Baseado em IEEE 730-2014 §4.7 (Software Reviews) e §4.8 (Software Audits)**

Todo PR em domínios de pagamento segue o **Protocolo de Revisão em Duas Etapas**:

1. **Revisão Técnica (Peer Review Bloqueador):** Aprovação de 1 engenheiro sênior obrigatória. Checklist mínimo:
   - [ ] Caminho feliz coberto por teste de unidade
   - [ ] Caminho de exceção (saldo insuficiente, timeout, cupom expirado) coberto
   - [ ] Nenhuma lógica de UI nos domínios `payment`, `coupon`, `checkout`
   - [ ] Método com mais de 20 linhas foi questionado e justificado

2. **Auditoria de Pipeline (Automated Gate):** CI deve passar `green` em todos os jobs antes que o botão de merge seja habilitado pelo GitHub Branch Protection.

---

## 6. Gestão de Configuração e Rastreabilidade

**Baseado em IEEE 730-2014 §4.9 (Software Configuration Management)**

```
main ──────────────────────────────────────────► produção (tag vX.Y.Z)
  └── develop ────────────────────────────────► staging
        └── feature/QG-coupon-balance-fix ───► PR com Quality Gate
```

- **Commits** seguem Conventional Commits: `fix(coupon): validate card balance before dispatch`
- **Tags de versão** semânticas: `v2.1.0` — nunca merge direto em `main` sem PR aprovado
- **Rastreabilidade:** Cada commit referencia o ID do risco mitigado no corpo da mensagem (`Mitigates: R1`)

---

## 7. Snippet Refatorado — Clean Code

O código abaixo representa a refatoração da lógica defeituosa da Black Friday sob as normativas de **Clean Code** (nomenclatura, SRP, ausência de side effects em validadores).

O arquivo completo está em:
`src/main/java/com/shopfast/coupon/CouponValidationService.java`

```java
// ANTES (Código da Black Friday — Violação de SRP + ausência de validação de saldo)
public boolean aplicarCupom(String cod, CartaoCredito c) {
    if (banco.getCupom(cod) != null) {
        tela.exibirBarraVerde();          // ← UI acoplada ao validador financeiro
        logistica.despachar();            // ← Despacho sem confirmação de pagamento
        return true;
    }
    return false;
}

// DEPOIS (Refatorado — Clean Code + SRP + validação completa)
// Ver: src/main/java/com/shopfast/coupon/CouponValidationService.java
```

---

## 8. Referências

- IEEE 730-2014. *Standard for Software Quality Assurance Processes*. IEEE, 2014.
- BARTIÉ, A. *Garantia da Qualidade de Software*. Elsevier. Caps. 3 e 5.
- PRESSMAN, R. S. *Engenharia de Software: Uma Abordagem Profissional*. 8.ed. AMGH, 2016. Cap. 16.
- SOMMERVILLE, I. *Engenharia de Software*. 10.ed. Pearson, 2018. Cap. 24.
- PAGE, A. "Quality Isn't a Testing Problem." *The Weasel Speaks* (Substack), mar. 2026. moderntesting.org
- RAHMAN, H. et al. "Next-Generation Software Quality Assurance." *Journal of Computer Science and Technology Studies*, v.7, n.9, pp.453–463, 2025. DOI: 10.32996/jcsts.2025.7.9.52
- RUBENFELD, C. "The coming explosion in QA testing." *Charles Rubenfeld's Newsletter*, jan. 2025.
- PAGE, A.; JENSEN, B. *Modern Testing Principles*. moderntesting.org

---

> *"Qualidade não é um problema de teste — é um problema de sistema. Adicionar mais testes a uma fábrica disfuncional apenas documenta a ausência de qualidade com mais eficiência."*
> — Alan Page, 2026
