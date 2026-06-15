# 📋 Especificação Funcional — Gestão de Assembleias (assembleia)

*   **Status**: IMPLEMENTED
*   **Versão**: v1.0 (Baseline Retroativo)
*   **Aprovações**: Especialista Consórcios [✅] (2026-06-14) | Especialista Contabilidade [✅] (2026-06-14)
*   **Última alteração**: Criação do baseline de assembleias e fluxo de captação de lances.

---

## 🎯 Objetivo
Gerenciar o ciclo de vida das Assembleias Gerais Ordinárias (AGO) dos grupos de consórcio, servindo como o evento central onde ocorrem as ofertas e homologações de lances e sorteios de contemplação.

---

## 🧮 Regras de Negócio

### REQ-ASM-001: Estados da Assembleia
- **Regra**: Uma assembleia transita pelos seguintes status:
  - `CAPTANDO`: Período em que o sistema permite o cadastro de propostas de lances pelos consorciados.
  - `REALIZADA`: A assembleia ocorreu, os sorteios e lances foram processados, mas o resultado financeiro/contemplações ainda está pendente de homologação definitiva (ex: aguarda integralização física de recursos do lance livre).
  - `FECHADA`: Assembleia encerrada e homologada de forma definitiva. Nenhum resultado ou lance desta AGO pode ser alterado.

### REQ-ASM-002: Vinculação e Frequência
- **Regra**: Cada assembleia pertence a um único grupo e possui uma data fixada (`dataAssembleia`). Não pode haver duas assembleias ativas (`CAPTANDO` ou `REALIZADA`) concorrentemente para o mesmo grupo.

---

## 🎯 Critérios de Aceitação (Given/When/Then)

### REQ-ASM-001 - AC1: Abertura de Captação
- **Given**: Um grupo de consórcio ativo no status `EM_ANDAMENTO`.
- **When**: O operador do sistema agenda uma nova assembleia para o grupo.
- **Then**: O sistema cria a assembleia no status `CAPTANDO`, permitindo o recebimento de lances pelas cotas daquele grupo.

### REQ-ASM-001 - AC2: Impedimento de Assembleias Duplicadas
- **Given**: Uma assembleia no status `CAPTANDO` para o Grupo 100.
- **When**: O operador tenta agendar outra assembleia para o mesmo Grupo 100.
- **Then**: O sistema impede a operação, retornando erro com status `400 Bad Request` indicando que já há uma assembleia de captação ativa.

### REQ-ASM-001 - AC3: Fechamento da Assembleia
- **Given**: Uma assembleia no status `REALIZADA` cujo processamento contábil e de integralizações foi concluído.
- **When**: O operador solicita o fechamento definitivo da AGO.
- **Then**: O status da assembleia passa para `FECHADA` e novas alterações ou cadastros de lances são bloqueados.
