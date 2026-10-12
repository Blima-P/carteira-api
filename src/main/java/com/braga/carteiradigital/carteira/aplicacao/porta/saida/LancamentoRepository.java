package com.braga.carteiradigital.carteira.aplicacao.porta.saida;

import java.util.Optional;
import java.util.UUID;

import com.braga.carteiradigital.carteira.dominio.Lancamento;
import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

public interface LancamentoRepository {

    void registrar(Lancamento lancamento);

    /** O saldo registrado no lançamento da transação nessa carteira. */
    Optional<Dinheiro> buscarSaldoApos(UUID carteiraId, UUID transacaoId);
}
