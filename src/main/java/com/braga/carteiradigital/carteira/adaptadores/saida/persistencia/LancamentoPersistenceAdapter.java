package com.braga.carteiradigital.carteira.adaptadores.saida.persistencia;

import org.springframework.stereotype.Component;

import com.braga.carteiradigital.carteira.aplicacao.porta.saida.LancamentoRepository;
import com.braga.carteiradigital.carteira.dominio.Lancamento;

@Component
class LancamentoPersistenceAdapter implements LancamentoRepository {

    private final LancamentoJpaRepository jpa;

    LancamentoPersistenceAdapter(LancamentoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void registrar(Lancamento lancamento) {
        jpa.save(LancamentoEntity.de(lancamento));
    }
}
