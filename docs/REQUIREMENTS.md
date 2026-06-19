# 📋 Índice de Requisitos e Modelagem Global — Consórcio API

Este documento serve como índice para as especificações técnicas modulares (OpenSpec) e centraliza a definição das entidades de domínio e enums globais compartilhados por todo o sistema de gestão de consórcios, em conformidade com a Lei Federal nº 11.795/08 e a Resolução BCB nº 285/2023.

---

## 🗂️ 1. Índice de Capabilities (OpenSpec)

Cada funcionalidade do sistema possui sua própria especificação técnica isolada contendo requisitos, regras de negócio e critérios de aceitação específicos:

1. [Autenticação e Sessão (auth)](file:///f:/Dev/Projetos/consorcio-api/docs/specs/auth/spec.md) — Segurança, cookies HttpOnly, sessão ativa (F5-safety).
2. [Gestão de Assembleias (assembleia)](file:///f:/Dev/Projetos/consorcio-api/docs/specs/assembleia/spec.md) — Criação de AGOs, captação de ofertas e fechamento.
3. [Composição de Fundos e Parcelas (fundos)](file:///f:/Dev/Projetos/consorcio-api/docs/specs/fundos/spec.md) — Motores de cálculo de parcelas (FC, TA, FR, Seguros).
4. [Oferta de Lances (lances)](file:///f:/Dev/Projetos/consorcio-api/docs/specs/lances/spec.md) — Registro de propostas de lances livres, embutidos e amortização de saldos.
5. [Apuração e Contemplações (contemplacao)](file:///f:/Dev/Projetos/consorcio-api/docs/specs/contemplacao/spec.md) — Ordenação de lances, sorteio, verificação contábil e integralização.
6. [Mora e Inadimplência (inadimplencia)](file:///f:/Dev/Projetos/consorcio-api/docs/specs/inadimplencia/spec.md) — Encargos moratórios, juros *pro rata die* e multas revertidas ao grupo.
7. [Seguros (seguros)](file:///f:/Dev/Projetos/consorcio-api/docs/specs/seguros/spec.md) — Cobertura prestamista e de quebra de garantia.
8. [Restituição de Excluídos (excluidos)](file:///f:/Dev/Projetos/consorcio-api/docs/specs/excluidos/spec.md) — Devoluções legais pelo bem atualizado e multa rescisória (ADR 005).
9. [Reajustes e Encerramento (encerramento)](file:///f:/Dev/Projetos/consorcio-api/docs/specs/encerramento/spec.md) — Reajuste do bem de referência e encerramento contábil com RNP (ADR 006).
10. [Relatórios e PLD/FT (relatorios)](file:///f:/Dev/Projetos/consorcio-api/docs/specs/relatorios/spec.md) — Monitoramento de lances suspeitos, estatísticas gerenciais (Doc 2080) e balancete contábil (Doc 4110).

---

## 🏗️ 2. Modelo de Dados e Entidades Globais (Domain/Model)

As entidades globais mapeadas abaixo representam a estrutura de dados compartilhada pelas diversas capabilities:

### A. Entidades do Consórcio

#### 1. `Cliente`
Cadastro unificado do participante do consórcio.
- **Atributos**:
  - `id` (Long, PK auto-incremental)
  - `cpfCnpj` (String, único, indexado, formato estrito)
  - `nome` (String, obrigatório)
  - `email` (String, obrigatório, único)
  - `rendaMensalDeclarada` (BigDecimal, para heurísticas de PLD/FT)
  - `patrimonioEstimado` (BigDecimal, para heurísticas de PLD/FT)
  - `status` (Enum: `ATIVO`, `INATIVO`)

#### 2. `Grupo`
Pool coletivo autônomo com patrimônio de afetação segregado da administradora.
- **Atributos**:
  - `id` (Long, PK)
  - `codigo` (String, único, indexado)
  - `valorCredito` (BigDecimal, valor do bem de referência)
  - `prazoMeses` (Integer, total de meses do grupo)
  - `taxaAdministracao` (BigDecimal, taxa total ex: 15.00%)
  - `fundoReserva` (BigDecimal, taxa total ex: 2.00%)
  - `percentualLanceEmbutidoMaximo` (BigDecimal, ex: 30.00%)
  - `percentualLanceFixo` (BigDecimal, ex: 20.00%)
  - `status` (Enum: `EM_FORMACAO`, `EM_ANDAMENTO`, `ENCERRADO`)
  - `criterioDesempateLance` (Enum: `COTA_MAIS_PROXIMA_SORTEADA`, `DATA_OFERTA`)

#### 3. `Cota`
Contrato individualizado de participação dentro de um grupo.
- **Atributos**:
  - `id` (Long, PK)
  - `numeroCota` (Integer, obrigatório, único por grupo)
  - `cliente` (Relationship `@ManyToOne` com `Cliente`)
  - `grupo` (Relationship `@ManyToOne` com `Grupo`)
  - `status` (Enum: `ATIVA`, `CANCELADA`, `AGUARDANDO_ANALISE`, `APROVADO`)
  - `valorReembolsado` (BigDecimal, acumulador de reembolsos pagos)
  - `reembolsada` (Boolean, flag indicativa de quitação de reembolso)
  - `version` (Long, `@Version` para concorrência otimista)

#### 4. `Parcela`
Títulos de faturamento mensal gerados para cada cota.
- **Atributos**:
  - `id` (Long, PK)
  - `cota` (Relationship `@ManyToOne` com `Cota`)
  - `numeroParcela` (Integer, número da parcela no cronograma)
  - `valorFundoComum` (BigDecimal)
  - `valorTaxaAdministracao` (BigDecimal)
  - `valorFundoReserva` (BigDecimal)
  - `valorSeguro` (BigDecimal)
  - `valorMulta` (BigDecimal, mora 2%)
  - `valorJuros` (BigDecimal, mora 1% a.m. pro rata die)
  - `dataVencimento` (LocalDate)
  - `dataPagamento` (LocalDate, nulo se pendente)
  - `status` (Enum: `PENDENTE`, `PAGA`, `ATRASADA`)

#### 5. `Assembleia`
Assembleia Geral Ordinária (AGO) do grupo.
- **Atributos**:
  - `id` (Long, PK)
  - `grupo` (Relationship `@ManyToOne` com `Grupo`)
  - `dataAssembleia` (LocalDate, indexada)
  - `status` (Enum: `CAPTANDO`, `REALIZADA`, `FECHADA`)

#### 6. `Lance`
Propostas de antecipação de saldo para a AGO.
- **Atributos**:
  - `id` (Long, PK)
  - `cota` (Relationship `@ManyToOne` com `Cota`)
  - `assembleia` (Relationship `@ManyToOne` com `Assembleia`)
  - `tipo` (Enum: `EMBUTIDO`, `FIRME`, `MISTO`)
  - `modalidade` (Enum: `LIVRE`, `FIXO`)
  - `valorOferta` (BigDecimal)
  - `dataOferta` (LocalDateTime)
  - `statusApuracao` (Enum: `PENDENTE`, `VENCEDOR`, `PERDEDOR`)

#### 7. `Contemplacao`
Registro do direito creditório atribuído à cota.
- **Atributos**:
  - `id` (Long, PK)
  - `cota` (Relationship `@OneToOne` com `Cota`)
  - `assembleia` (Relationship `@ManyToOne` com `Assembleia`)
  - `tipoContemplacao` (Enum: `SORTEIO`, `LANCE_LIVRE`, `LANCE_FIXO`)
  - `valorCreditoLiberado` (BigDecimal)
  - `valorLance` (BigDecimal, zero se sorteio)
  - `lanceEmbutido` (Boolean)
  - `dataContemplacao` (LocalDate)
  - `status` (Enum: `PENDENTE_INTEGRALIZACAO`, `AGUARDANDO_ANALISE`, `APROVADO`)

---

## 🧮 3. Entidades Contábeis (Double-Entry Ledger)

A contabilidade de grupos adota partidas dobradas sob a estrutura do COSIF (BCB).

#### 1. `ContaContabil`
- **Atributos**:
  - `codigoContabil` (String, PK, padrão de 8 dígitos ex: `2.1.2.10.10-6`)
  - `descricao` (String)
  - `natureza` (Enum: `DEVEDORA`, `CREDORA`)
  - `tipo` (Enum: `ATIVO`, `PASSIVO`, `PATRIMONIO_LIQUIDO`, `RESULTADO`, `COMPENSACAO`)

#### 2. `LancamentoContabil`
- **Atributos**:
  - `id` (Long, PK)
  - `grupo` (Relationship `@ManyToOne` com `Grupo`)
  - `contaDebito` (Relationship `@ManyToOne` com `ContaContabil`)
  - `contaCredito` (Relationship `@ManyToOne` com `ContaContabil`)
  - `valor` (BigDecimal, positivo)
  - `dataLancamento` (LocalDateTime)
  - `historico` (String, descrição auditável da transação)
  - `referenciaOrigem` (String, identificador do evento originador ex: `CON-001`, `LAN-045`)

---

## 🔒 4. Entidades de Auditoria

#### 1. `HistoricoVersaoCota`
- **Atributos**:
  - `id` (Long, PK)
  - `cotaId` (Long, indexado)
  - `statusAnterior` (Enum `CotaStatus`)
  - `statusNovo` (Enum `CotaStatus`)
  - `usuarioResponsavel` (String, e-mail/CPF extraído do Spring Security context)
  - `dataAlteracao` (LocalDateTime)
  - `justificativa` (String, obrigatória)
