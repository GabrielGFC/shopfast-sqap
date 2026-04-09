## Descrição

> Descreva em 2–3 linhas **o que** foi alterado e **por quê**. Use linguagem de negócio.

---

## Tipo de Mudança

- [ ] 🐛 Bug fix (correção que não quebra compatibilidade)
- [ ] ✨ Nova feature (adição que não quebra compatibilidade)
- [ ] 💥 Breaking change (alteração que quebra compatibilidade)
- [ ] 🔒 Segurança (mitigação de risco identificado na Matriz P×I)
- [ ] ♻️ Refatoração (sem alteração de comportamento externo)
- [ ] 📚 Documentação

---

## ✅ Quality Gate Checklist (IEEE 730-2014 — Obrigatório)

> **Este PR só pode ser mergeado com TODOS os itens abaixo marcados.**
> O pipeline de CI bloqueia automaticamente se QG-01 ou QG-02 falhar.

### REGRA QG-01 — Cobertura de Testes (§4.4.2)
- [ ] Cobertura ≥ 90% nos pacotes `coupon`, `payment`, `checkout` (verificada pelo JaCoCo no CI)
- [ ] Caminho feliz coberto por teste de unidade
- [ ] **Caminho de exceção coberto** (saldo insuficiente, timeout, cupom expirado, código inválido)
- [ ] Cenário BDD adicionado/atualizado em `coupon_validation.feature` (se regra de negócio alterada)

### REGRA QG-02 — Arquitetura (§4.3.1)
- [ ] Nenhuma classe em `..payment..` ou `..coupon..` importa `..view..` ou `..ui..`
- [ ] ArchUnit está passando (`ArchitectureRulesTest`)
- [ ] Métodos com > 20 linhas foram questionados e justificados no PR

### Processo (IEEE 730-2014 §4.7 — Peer Review)
- [ ] Aprovação de pelo menos 1 engenheiro sênior
- [ ] Checkstyle passando (zero violations)
- [ ] Sem `TODO` ou `FIXME` não rastreados introduzidos

---

## Rastreabilidade de Risco

> Se este PR mitiga um risco da Matriz P×I, indique:

| ID Risco | Descrição | Ação |
|----------|-----------|------|
| R?       |           | Mitigado / Aceito / Transferido |

---

## Evidência de Teste

> Cole o output do `mvn verify` ou link para o run do CI:

```
Tests run: X, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
