# 📋 Especificação Funcional — Oferta de Lances (lances)

*   **Status**: IMPLEMENTED
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Aprovações**: Especialista Consórcios [✅] (2026-06-14) | Especialista Contabilidade [✅] (2026-06-14)
*   **Última alteração**: Baseline de cadastro, validação e motores de amortização de lances (redução de prazo/diluição).

---

## 🎯 Objetivo
Definir as regras para cadastramento e validação de lances ofertados pelos consorciados para as assembleias, além dos motores de cálculo para amortização do saldo em caso de pagamento do lance.

---

## 🧮 Regras de Negócio

### REQ-LAN-001: Elegibilidade para Oferta de Lance
- **Regra**: Apenas cotas que atendam concorrentemente a todos os critérios abaixo podem ofertar lances para uma assembleia:
  1. O status da cota deve ser `ATIVA`. Cotas `CANCELADA` ou pendentes não podem participar.
  2. A cota deve estar **100% adimplente** (sem nenhuma parcela com status `PENDENTE` ou `ATRASADA` vencida até a data da assembleia).
  3. A assembleia do grupo deve estar no status `CAPTANDO`.

### REQ-LAN-002: Modalidade de Lance Embutido
- **Regra**: O consorciado pode utilizar parte do próprio crédito como lance (Lance Embutido).
- **Limite**: O valor ofertado nesta modalidade não pode exceder o percentual teto definido no grupo (`percentualLanceEmbutidoMaximo` aplicado sobre o `valorCredito` do grupo).

### REQ-LAN-003: Motor de Amortização de Lances
Caso o lance seja vencedor e pago pelo consorciado, o saldo pode ser amortizado de duas formas, conforme escolha do cliente:
1. **Redução de Prazo (Quitação de trás para frente)**:
   - O lance quita as parcelas em aberto em ordem cronológica inversa (da última parcela do cronograma até a primeira).
   - As frações de **Taxa de Administração** e **Fundo de Reserva** das parcelas quitadas não podem ser perdidas pela administradora. Elas devem ser diluídas (redistribuídas uniformemente) sobre as parcelas remanescentes que continuam ativas.
2. **Diluição de Valor (Redução da prestação)**:
   - O saldo amortizado é dividido pelo número total de parcelas restantes. O valor de cada parcela restante é reduzido uniformemente.
   - **Regra do Centavo Perdido**: O motor de cálculo deve aplicar o ajuste de arredondamento de dízimas na última parcela restante para garantir que a soma das parcelas amortizadas seja idêntica ao saldo devedor restante do grupo.

---

## 🎯 Critérios de Aceitação (Given/When/Then)

### REQ-LAN-001 - AC1: Validação de Inadimplência na Oferta
- **Given**: Uma cota com uma parcela vencida há 5 dias (status `ATRASADA`).
- **When**: O cliente tenta cadastrar uma proposta de lance para a assembleia no status `CAPTANDO`.
- **Then**: O sistema impede o cadastro do lance, retornando erro de validação com status `400 Bad Request` indicando inadimplência ativa.

### REQ-LAN-002 - AC1: Limite de Lance Embutido
- **Given**: Um grupo com crédito de R$ 100.000,00 e percentual máximo de lance embutido de 30% (teto de R$ 30.000,00).
- **When**: O consorciado envia uma proposta de lance embutido de R$ 35.000,00.
- **Then**: O sistema rejeita o lance por ultrapassar o limite configurado no grupo.

### REQ-LAN-003 - AC1: Amortização com Redução de Prazo e Diluição de Taxas
- **Given**: Uma cota com 10 parcelas restantes, cada uma contendo R$ 100,00 de fundo comum e R$ 15,00 de taxa de administração.
- **When**: Um lance de R$ 200,00 (referente a 2 parcelas de fundo comum) é amortizado via redução de prazo.
- **Then**: O sistema remove as 2 últimas parcelas e dilui o valor correspondente às taxas de administração delas (R$ 30,00 no total) entre as 8 parcelas remanescentes, adicionando R$ 3,75 a cada parcela ativa.
