# 📂 Relatório de Auditoria UI/UX — Sistema de Consórcios

Este relatório apresenta uma análise crítica e técnica do design visual, usabilidade, responsividade e acessibilidade (WCAG AA) do frontend (`front_end_consorcio-api`). Foram inspecionadas as 13 telas do painel backoffice e os formulários interativos.

---

## 🎨 1. Incoerências com o Design System Base

O design system estabelecido na pasta `docs/` e na identidade de marca prevê:
*   **Tema Escuro:** Fundo primário (`bg-primary`) `#0F172A`, fundo do card/painel (`bg-card`) `#1E293B`, e acento `#F59E0B` (Amber).
*   **Tipografia:** `Space Grotesk` (títulos) + `Inter` (corpo).
*   **Aparência:** Glassmorphism, micro-animações, hover effects e feedbacks fluidos.

### Inconsistências Encontradas:
1.  **Paleta de Cores e Acento Conflitante:**
    *   No `index.css`, as cores base diferem do padrão: `--bg-main` está `#0b0f19` (um azul escuro quase preto) e `--bg-card` está como `rgba(22, 30, 49, 0.6)`.
    *   A acento primário está configurado no CSS como `--primary: #6366f1` (Indigo). Em telas como `LancesPendentesPage.jsx` e `ReembolsosExcluidosPage.jsx`, usam-se classes Tailwind de cor Âmbar (`text-amber-500`, `text-amber-400`). Isso gera uma interface com dupla identidade visual (Índigo e Âmbar competindo por ações principais e destaque).
2.  **Fonte Ausente e Incorreta:**
    *   No `index.css`, a fonte importada do Google Fonts é a `Outfit` e `Inter` (não há menção ou importação de `Space Grotesk`).
    *   A variável `--font-title` está configurada para usar `'Outfit'`, fazendo com que todos os títulos do painel quebrem a diretriz tipográfica.
3.  **Diálogos e Modais Quebrando a Identidade Visual:**
    *   As telas de `CotasPage.jsx` e `GruposPage.jsx` usam diálogos nativos do navegador (`alert`, `confirm`, `prompt`). Além dos graves problemas de usabilidade, esses diálogos são renderizados em branco padrão pelo navegador, quebrando o Glassmorphism e o tema escuro do painel.

---

## ⚡ 2. Severidades de Usabilidade (UX) e Arquitetura Visual

Classificamos os problemas encontrados em três níveis de severidade: **Grave** (bloqueia ou prejudica criticamente a operação/acessibilidade), **Médio** (afeta usabilidade e consistência) e **Cosmético** (detalhes visuais e polimento).

### 🔴 SEVERIDADE GRAVE

#### A. Uso de Diálogos Nativos (`alert`, `confirm`, `prompt`)
*   **Ocorrências:**
    *   `CotasPage.jsx`: Usa `confirm` na exclusão/cancelamento de cotas e `alert` para exibir a memória de cálculo e simulação de reembolso.
    *   `GruposPage.jsx`: Usa `prompt` para solicitar a data da primeira AGO no momento de inaugurar o grupo e outro `prompt` para receber o novo valor no reajuste de crédito.
*   **Problema:** Bloqueia a thread de execução do navegador, impede a navegação acessível por teclado, não possui validação nativa de formato de data/moeda (como os esquemas do Zod) e exibe janelas brancas em total desacordo com o tema escuro e glassmorphism.
*   **Correção Proposta:** Substituir por modais React com inputs estilizados ou reutilizar a estrutura do `ConfirmDialog.jsx`.

#### B. Gestão de Estado Local para Dados do Servidor (`DashboardPage.jsx`)
*   **Ocorrências:**
    *   `DashboardPage.jsx`: Realiza buscas nas APIs de clientes, grupos, cotas e financeiro dentro de um `useEffect` local e joga o resultado em um `useState` chamado `stats`.
*   **Problema:** Viola a diretriz arquitetural inegociável de manter todo o estado do servidor sob gerência exclusiva do **TanStack Query**. Causa buscas redundantes ao recarregar a tela (sem cache), impede a sincronização em tempo real após ações em outras páginas e carece de feedbacks fluidos de carregamento integrados (loading skeletons/error states).
*   **Correção Proposta:** Refatorar a página para utilizar hooks customizados `useQuery` de forma paralela.

#### C. Páginas em Branco (Placeholders Completos)
*   **Ocorrências:** `AssembleiasPage.jsx` e `FinanceiroPage.jsx`.
*   **Problema:** O operador de backoffice não consegue executar amortizações, emitir boletos ou abrir assembleias ordinárias (AGO) porque estas telas contêm apenas textos informativos estáticos de que "o módulo está sendo integrado".
*   **Correção Proposta:** Desenvolver a interface visual de simulação de amortizações e o painel de apuração de lances em assembleia.

#### D. Associação Incorreta de Acessibilidade (Labels e Inputs)
*   **Ocorrências:** `ClienteForm.jsx`, `GrupoForm.jsx` e `CotaForm.jsx`.
*   **Problema:** Nenhuma das tags `<label>` possui o atributo `htmlFor` apontando para o correspondente `id` do `<input>`. A maioria dos campos de entrada de dados do formulário sequer possui uma propriedade `id`.
    *   No `ClienteForm.jsx`, a label `<label>Localidade / UF</label>` agrupa visualmente os inputs `localidade` e `uf` sem identificação acessível individual.
    *   A label `<label htmlFor="logradouro">Logradouro Completo</label>` engloba os inputs de `logradouro` e `numero` (o número fica sem label).
*   **Problema:** Leitores de tela não conseguem ditar a legenda do input quando focado, impedindo o preenchimento por pessoas com deficiência visual.
*   **Correção Proposta:** Corrigir as tags JSX associando atributos `id` aos inputs e `htmlFor` às labels.

---

### 🟡 SEVERIDADE MÉDIA

#### A. Ausência de Focus Trap nos Modais (WCAG AA 2.4.3)
*   **Ocorrências:** Todos os formulários abertos em modal (`ClienteForm.jsx`, `GrupoForm.jsx`, `CotaForm.jsx` e modais nas páginas de Lances e Reembolsos).
*   **Problema:** Quando um modal está aberto, o foco do teclado (Tab order) não fica retido no modal. O usuário consegue pressionar `Tab` sucessivamente até que o foco "vaze" e selecione botões ocultos por trás do modal (como os links do menu lateral na sidebar).
*   **Correção Proposta:** Utilizar uma biblioteca de acessibilidade (como `react-focus-lock` ou construir um hook nativo para aprisionar o foco do Tab).

#### B. Layout Não Responsivo (Mobile & Tablet)
*   **Ocorrências:** Estrutura base de layouts e grades do painel.
*   **Problema:** A `sidebar` possui largura fixa de 260px e não é colapsável. O conteúdo principal (`main-content`) tem largura definida como `max-width: calc(100vw - var(--sidebar-width))`. Em resoluções móveis (menores que 1024px), a sidebar esmaga o conteúdo principal.
    *   Em `EncerrarGrupoPage.jsx` e `RelatorioBalancetePage.jsx`, os grids estão fixados no inline CSS como `gridTemplateColumns: '1fr 1fr 1fr'`. Em telas pequenas, as métricas e dados de caixa ficam esmagados e sobrepostos.
*   **Correção Proposta:** Adicionar breakpoints responsivos na Sidebar (sidebar colapsável/drawer) e refatorar os grids inline para classes CSS responsivas do Tailwind (ex: `grid grid-cols-1 md:grid-cols-3 gap-6`).

#### C. Contraste de Cores em Feedbacks e Erros (WCAG AA 1.4.3)
*   **Ocorrências:** Textos de erro inline e badges de risco/alerta vermelhos.
*   **Problema:** Mensagens de erro de formulário usam `#ef4444` (vermelho puro) diretamente sobre fundos escuros (`#0b0f19`). O contraste não atinge a razão mínima de 4.5:1 exigida para leitura legível.
*   **Correção Proposta:** Ajustar a cor de erro em tema escuro para tons mais claros (como `text-rose-400` ou `text-red-300`).

#### D. Emojis Puros sem Descrição de Acessibilidade
*   **Ocorrências:** Ações de tabelas em `ClientesPage.jsx` (botões `📜` e `🗑️`).
*   **Problema:** Botões interativos contendo apenas caracteres de emoji não possuem o atributo `aria-label`. Leitores de tela pronunciam "pergaminho" e "lixeira", dificultando o entendimento da ação correspondente.
*   **Correção Proposta:** Adicionar `aria-label="Visualizar Histórico de Auditoria"` e `aria-label="Inativar Consorciado"` aos botões.

---

### 🟢 SEVERIDADE COSMÉTICA

#### A. Máscara de CPF / CNPJ e CEP nos Campos de Texto
*   **Ocorrências:** `ClienteForm.jsx`.
*   **Problema:** O input de CPF/CNPJ e o de CEP exigem apenas números crus e não fornecem máscara visual de digitação (ex: `000.000.000-00` ou `00.000-000`). Isso aumenta a taxa de erro de preenchimento dos operadores.
*   **Correção Proposta:** Implementar máscara dinâmica baseada no tamanho da string digitada.

#### B. Valor Fixo no Fundo Comum (Tabela de Grupos)
*   **Ocorrências:** `GruposPage.jsx` (linha 90).
*   **Problema:** A coluna do Saldo do Fundo Comum exibe `--` de forma fixa para todos os grupos, sem representar o valor real.
*   **Correção Proposta:** Mapear o dado retornado na API de saldo de caixa contábil do grupo.

---

## 🛠️ 3. Propostas de Refatoração de Código (JSX & CSS)

### A. Substituição de CSS de Cores e Fontes Base no `index.css`
Ajuste das variáveis de temas escuros e tipografia no arquivo de estilo global:

```diff
- @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Outfit:wght@400;500;600;700;800&display=swap');
+ @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=Space+Grotesk:wght@400;500;600;700&display=swap');

  :root {
    /* Font Families */
-   --font-title: 'Outfit', 'Inter', system-ui, -apple-system, sans-serif;
+   --font-title: 'Space Grotesk', 'Inter', system-ui, -apple-system, sans-serif;
    --font-body: 'Inter', system-ui, -apple-system, sans-serif;

    /* Color Palette - Premium Dark Theme (Harmonizado com a diretriz do projeto) */
-   --bg-main: #0b0f19;
-   --bg-sidebar: rgba(15, 22, 38, 0.7);
-   --bg-card: rgba(22, 30, 49, 0.6);
+   --bg-main: #0F172A; /* Slate-900 */
+   --bg-sidebar: rgba(30, 41, 59, 0.8); /* Slate-800 com opacidade */
+   --bg-card: #1E293B; /* Slate-800 */
    --bg-card-hover: rgba(28, 38, 62, 0.8);
    --bg-input: rgba(13, 19, 33, 0.8);
    --bg-modal: rgba(15, 23, 42, 0.95);
    
    /* Brand Accents */
-   --primary: #6366f1;         /* Indigo */
-   --primary-hover: #4f46e5;
-   --primary-glow: rgba(99, 102, 241, 0.35);
+   --primary: #F59E0B;         /* Amber (Acento Oficial) */
+   --primary-hover: #d97706;
+   --primary-glow: rgba(245, 158, 11, 0.35);
  }
```

---

### B. Refatoração de usabilidade no `CotasPage.jsx`
Remoção do `confirm()` e `alert()` nativos para utilizar modais interativos e estilizados.

```diff
-   const handleCancelarCota = (id) => {
-     if (confirm("Deseja cancelar esta cota? As parcelas pendentes futuras serão excluídas de projeções conforme regra BCB.")) {
-       cancelarMutation.mutate(id);
-     }
-   };
+   // Substituição por ConfirmDialog
+   const [cotaCancelarId, setCotaCancelarId] = useState(null);
+
+   const triggerCancelar = (id) => {
+     setCotaCancelarId(id);
+   };
```

Adição no JSX do retorno de `CotasPage.jsx`:
```jsx
<ConfirmDialog 
  isOpen={cotaCancelarId !== null}
  title="Cancelar Cota Consorcial?"
  message="Atenção: Ao cancelar a cota, as parcelas pendentes futuras serão excluídas das projeções financeiras e a cota passará a concorrer nas AGOs sob a regra legal de cotas excluídas (ADR 005)."
  type="danger"
  confirmText="Confirmar Cancelamento"
  cancelText="Voltar"
  onConfirm={() => {
    cancelarMutation.mutate(cotaCancelarId);
    setCotaCancelarId(null);
  }}
  onCancel={() => setCotaCancelarId(null)}
/>
```

---

### C. Refatoração de usabilidade e arquitetura do `DashboardPage.jsx`
Adequação aos ganchos de consulta do **TanStack Query** para carregar os dados em paralelo e evitar ciclos de efeito imperativos:

```jsx
import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../services/api';
import { useToast } from '../context/ToastContext';

export const DashboardPage = () => {
  // Queries gerenciadas pelo TanStack Query (Estado do Servidor isolado)
  const { data: clientesData, isLoading: loadingClientes } = useQuery({
    queryKey: ['clientes'],
    queryFn: () => api.clientes.listar(0, 100)
  });

  const { data: gruposData, isLoading: loadingGrupos } = useQuery({
    queryKey: ['grupos'],
    queryFn: () => api.grupos.listar()
  });

  const { data: cotasData, isLoading: loadingCotas } = useQuery({
    queryKey: ['cotas'],
    queryFn: () => api.cotas.listar()
  });

  const loading = loadingClientes || loadingGrupos || loadingCotas;

  // Processamento e computação de valores em memória reativa (useMemo)
  const stats = React.useMemo(() => {
    const clientesList = clientesData?.content || clientesData || [];
    const totalClientes = Array.isArray(clientesList)
      ? clientesList.filter(c => c.statusCliente !== 'INATIVO').length
      : 0;

    const gruposList = gruposData?.content || gruposData || [];
    const totalGrupos = gruposList.length;

    const cotasList = cotasData?.content || cotasData || [];
    const totalCotas = Array.isArray(cotasList)
      ? cotasList.filter(c => c.status === 'ATIVA' || c.status === 'CONTEMPLADA').length
      : 0;

    return {
      clientes: totalClientes,
      grupos: totalGrupos,
      cotas: totalCotas,
      arrecadacao: 245380.50 // Mock para exibição (futuramente query de caixa)
    };
  }, [clientesData, gruposData, cotasData]);

  if (loading) {
    return (
      <div className="view-container animate-pulse space-y-6">
        <div className="h-8 bg-slate-800 rounded w-1/4"></div>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div className="h-32 bg-slate-800 rounded-xl"></div>
          <div className="h-32 bg-slate-800 rounded-xl"></div>
          <div className="h-32 bg-slate-850 rounded-xl"></div>
          <div className="h-32 bg-slate-850 rounded-xl"></div>
        </div>
      </div>
    );
  }

  // JSX de visualização reativa...
};
```

---

### D. Correção de Acessibilidade no `ClienteForm.jsx`
Associação explícita de labels e inputs com IDs individuais, e correção do contraste de texto das mensagens de erro.

```diff
-   <div className="form-group" style={{ gridColumn: 'span 2' }}>
-     <label>Nome / Razão Social *</label>
-     <input type="text" {...register('nome')} placeholder="Nome completo" />
-     {errors.nome && <span className="error-text" style={{ color: '#ef4444', fontSize: '0.8rem' }}>{errors.nome.message}</span>}
-   </div>
+   <div className="form-group" style={{ gridColumn: 'span 2' }}>
+     <label htmlFor="cliente-nome">Nome / Razão Social *</label>
+     <input 
+       id="cliente-nome" 
+       type="text" 
+       {...register('nome')} 
+       placeholder="Nome completo" 
+       aria-required="true"
+       aria-invalid={!!errors.nome}
+       aria-describedby={errors.nome ? "error-nome" : undefined}
+     />
+     {errors.nome && (
+       <span 
+         id="error-nome" 
+         className="text-rose-400 text-xs mt-1 block"
+       >
+         {errors.nome.message}
+       </span>
+     )}
+   </div>
```

---

## 📋 4. Checklist de Correção para os Desenvolvedores

| Tela / Componente | Descrição do Problema | Severidade | Ação Necessária |
| :--- | :--- | :--- | :--- |
| **Global (`index.css`)** | Configuração errada de cores base e fonte Outfit no lugar de Space Grotesk | Cosmético | Ajustar variáveis de cores no `:root` e importar Space Grotesk |
| **`LoginPage.jsx`** | Faltam aria-labels e foco inicial ao carregar a página | Médio | Focar automaticamente no campo "Login Administrativo" |
| **`DashboardPage.jsx`** | Uso de `useEffect`/`useState` para chamadas de API | **Grave** | Refatorar para utilizar TanStack Query |
| **`CotasPage.jsx`** | Diálogos nativos `confirm` e `alert` para processos e cancelamentos | **Grave** | Implementar `ConfirmDialog` e modal de simulação |
| **`GruposPage.jsx`** | Diálogos nativos `prompt` para receber datas e moedas | **Grave** | Substituir por modais estilizados e controlados |
| **`ClienteForm.jsx`** | Inputs sem ID e labels agrupadas sem indexação acessível | **Grave** | Adicionar `htmlFor` e `id` explícitos em todos os campos |
| **`GrupoForm.jsx`** | Labels sem `htmlFor` e inputs de números sem formatação de moeda | **Grave** | Corrigir IDs das labels e criar máscara ou componente monetário |
| **Modais (Geral)** | Falta de Focus Trap na navegação por teclado | Médio | Prender foco do teclado dentro do modal ativo |
| **Layout Base** | Sidebar fixa esmaga o painel em telas menores | Médio | Tornar sidebar retrátil ou adicionar menu mobile responsivo |
| **Placeholders** | Páginas de Assembleias e Amortizações vazias | **Grave** | Desenvolver componentes interativos de simulação e apuração |
