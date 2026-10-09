package com.braga.carteiradigital.carteira.aplicacao.porta.saida;

import com.braga.carteiradigital.carteira.dominio.Lancamento;

public interface LancamentoRepository {

    void registrar(Lancamento lancamento);
}
