package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Grupo;
import br.com.estudo.consorcio.domain.model.NaturezaMovimento;
import br.com.estudo.consorcio.domain.model.RendimentoFinanceiro;
import br.com.estudo.consorcio.domain.model.TipoMovimentoFinanceiro;
import br.com.estudo.consorcio.domain.model.Usuario;
import br.com.estudo.consorcio.domain.repository.GrupoRepository;
import br.com.estudo.consorcio.domain.repository.RendimentoFinanceiroRepository;
import br.com.estudo.consorcio.exception.RegraDeNegocioException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class RendimentoFinanceiroService {

    private final RendimentoFinanceiroRepository rendimentoRepository;
    private final GrupoRepository grupoRepository;
    private final MovimentoFinanceiroService movimentoService;
    private final ContabilidadeService contabilidadeService;

    public RendimentoFinanceiroService(RendimentoFinanceiroRepository rendimentoRepository,
                                       GrupoRepository grupoRepository,
                                       MovimentoFinanceiroService movimentoService,
                                       ContabilidadeService contabilidadeService) {
        this.rendimentoRepository = rendimentoRepository;
        this.grupoRepository = grupoRepository;
        this.movimentoService = movimentoService;
        this.contabilidadeService = contabilidadeService;
    }

    private Usuario getUsuarioAutenticado() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario u) {
            return u;
        }
        return null;
    }

    @Transactional
    public RendimentoFinanceiro registrarRendimento(Long grupoId, BigDecimal valorRendimento, LocalDate dataRendimento, String descricao) {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RegraDeNegocioException("Grupo não encontrado."));

        if (valorRendimento.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Valor do rendimento deve ser maior que zero.");
        }

        RendimentoFinanceiro rendimento = RendimentoFinanceiro.builder()
                .grupo(grupo)
                .valorRendimento(valorRendimento)
                .dataRendimento(dataRendimento)
                .descricao(descricao)
                .build();

        rendimento = rendimentoRepository.save(rendimento);

        Usuario usuario = getUsuarioAutenticado();

        // Registrar no ledger contábil:
        // Crédito no Fundo de Reserva, Débito no Banco/Investimentos (Caixa para simplificar)
        contabilidadeService.registrarBaixa(
                grupo, null, null,
                ContabilidadeService.CONTA_CAIXA, // Aplicações
                ContabilidadeService.CONTA_FUNDO_RESERVA, // Rendimentos FR
                valorRendimento, dataRendimento,
                "Rendimento Financeiro: " + descricao
        );

        // Registrar no extrato/movimento
        movimentoService.registrarMovimento(
                grupo, null, null, null,
                TipoMovimentoFinanceiro.LANCAMENTO_MANUAL, // Ou criar RENDIMENTO_FINANCEIRO
                NaturezaMovimento.CREDITO,
                valorRendimento,
                "Rendimento Financeiro: " + descricao,
                usuario
        );

        return rendimento;
    }
}
