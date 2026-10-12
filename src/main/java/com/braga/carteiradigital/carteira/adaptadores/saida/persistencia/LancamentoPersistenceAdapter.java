package com.braga.carteiradigital.carteira.adaptadores.saida.persistencia;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.braga.carteiradigital.carteira.aplicacao.porta.saida.LancamentoRepository;
import com.braga.carteiradigital.carteira.dominio.Lancamento;
import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

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

    @Override
    public Optional<Dinheiro> buscarSaldoApos(UUID carteiraId, UUID transacaoId) {
        return jpa.buscarSaldoApos(carteiraId, transacaoId).map(Dinheiro::new);
    }
}
