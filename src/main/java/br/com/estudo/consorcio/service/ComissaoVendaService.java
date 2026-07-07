package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.ComissaoVenda;
import br.com.estudo.consorcio.domain.repository.ComissaoVendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ComissaoVendaService {

    private final ComissaoVendaRepository comissaoRepository;

    public Optional<ComissaoVenda> buscarPorContratoEStatus(Long contratoId, String status) {
        return comissaoRepository.findByContratoIdAndStatus(contratoId, status);
    }

    @Transactional
    public void pagarComissao(ComissaoVenda comissao) {
        comissao.setStatus("PAGA");
        comissaoRepository.save(comissao);
    }

    @Transactional
    public void estornarComissao(ComissaoVenda comissao) {
        comissao.setStatus("ESTORNADA_PARCIALMENTE");
        comissaoRepository.save(comissao);
    }
    
    @Transactional
    public ComissaoVenda criarComissaoPendente(br.com.estudo.consorcio.domain.model.Corretor corretor, 
            br.com.estudo.consorcio.domain.model.ContratoAdesao contrato, java.math.BigDecimal valor) {
        ComissaoVenda com = ComissaoVenda.builder()
                .corretor(corretor)
                .contrato(contrato)
                .valorTotalComissao(valor)
                .status("PENDENTE")
                .build();
        return comissaoRepository.save(com);
    }
}
