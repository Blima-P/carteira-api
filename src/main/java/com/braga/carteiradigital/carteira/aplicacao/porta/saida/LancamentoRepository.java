package com.braga.carteiradigital.carteira.aplicacao.porta.saida;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.braga.carteiradigital.carteira.LancamentoDaCarteira;
import com.braga.carteiradigital.carteira.PosicaoNoLivroRazao;
import com.braga.carteiradigital.carteira.dominio.Lancamento;
import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

public interface LancamentoRepository {

    void registrar(Lancamento lancamento);

    /** O saldo registrado no lançamento da transação nessa carteira. */
    Optional<Dinheiro> buscarSaldoApos(UUID carteiraId, UUID transacaoId);

    /** Lançamentos do mais recente para o mais antigo, a partir de {@code depoisDe} (exclusivo) quando informado. */
    List<LancamentoDaCarteira> buscarPagina(UUID carteiraId, PosicaoNoLivroRazao depoisDe, int limite);
}
