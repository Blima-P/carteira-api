package com.braga.carteiradigital.transacao.aplicacao.porta.saida;

import java.util.Optional;
import java.util.UUID;

import com.braga.carteiradigital.transacao.dominio.ChaveIdempotencia;
import com.braga.carteiradigital.transacao.dominio.Transacao;

public interface TransacaoRepository {

    /** Lança {@code ChaveIdempotenciaEmUsoException} se outra transação com a mesma chave foi gravada antes. */
    void salvar(Transacao transacao);

    Optional<Transacao> buscarPorChave(UUID iniciadaPor, ChaveIdempotencia chave);
}
