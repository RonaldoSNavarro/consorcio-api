package br.com.estudo.consorcio.service;

import br.com.estudo.consorcio.domain.model.Corretor;
import br.com.estudo.consorcio.domain.repository.CorretorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CorretorService {

    private final CorretorRepository corretorRepository;

    @Transactional
    public void adicionarDividaClawback(Corretor corretor, BigDecimal valorEstorno) {
        if (corretor != null && valorEstorno != null) {
            BigDecimal saldoAtual = corretor.getSaldoDevedor() != null ? corretor.getSaldoDevedor() : BigDecimal.ZERO;
            corretor.setSaldoDevedor(saldoAtual.add(valorEstorno));
            corretorRepository.save(corretor);
        }
    }
}
