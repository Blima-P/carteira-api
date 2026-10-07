package com.braga.carteiradigital.transacao.aplicacao.porta.saida;

import java.util.UUID;

import com.braga.carteiradigital.transacao.dominio.ChaveIdempotencia;
import com.braga.carteiradigital.transacao.dominio.Transacao;

public interface TransacaoRepository {

    void salvar(Transacao transacao);

    boolean existePorChave(UUID iniciadaPor, ChaveIdempotencia chave);
}
