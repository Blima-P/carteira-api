package com.braga.carteiradigital.carteira.aplicacao;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.braga.carteiradigital.carteira.CarteiraApi;
import com.braga.carteiradigital.carteira.aplicacao.porta.saida.CarteiraRepository;
import com.braga.carteiradigital.carteira.aplicacao.porta.saida.LancamentoRepository;
import com.braga.carteiradigital.carteira.dominio.Carteira;
import com.braga.carteiradigital.carteira.dominio.CarteiraNaoEncontradaException;
import com.braga.carteiradigital.carteira.dominio.Movimentacao;
import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

/** Implementa a API pública do módulo. É o único lugar do sistema que altera saldos. */
@Service
class MovimentarCarteiraService implements CarteiraApi {

    private final CarteiraRepository carteiras;
    private final LancamentoRepository lancamentos;
    private final Clock relogio;

    MovimentarCarteiraService(CarteiraRepository carteiras, LancamentoRepository lancamentos, Clock relogio) {
        this.carteiras = carteiras;
        this.lancamentos = lancamentos;
        this.relogio = relogio;
    }

    @Override
    @Transactional(readOnly = true)
    public UUID idDaCarteiraDoUsuario(UUID usuarioId) {
        return carteiras.buscarIdPorUsuario(usuarioId).orElseThrow(CarteiraNaoEncontradaException::new);
    }

    // MANDATORY: falha se for chamado fora de uma transação, em vez de abrir uma transação própria
    // e gravar o crédito separado da operação que o originou
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Dinheiro creditar(UUID carteiraId, Dinheiro valor, UUID transacaoId) {
        Carteira carteira = carteiras.buscarPorIdComBloqueio(carteiraId).orElseThrow(CarteiraNaoEncontradaException::new);

        Movimentacao movimentacao = carteira.creditar(valor, transacaoId, relogio);
        carteiras.atualizar(movimentacao.carteira());
        lancamentos.registrar(movimentacao.lancamento());
        return movimentacao.carteira().saldo();
    }
}
