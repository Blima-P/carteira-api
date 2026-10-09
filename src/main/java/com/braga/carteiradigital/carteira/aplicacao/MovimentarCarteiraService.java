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
        Movimentacao credito = bloquear(carteiraId).creditar(valor, transacaoId, relogio);
        registrar(credito);
        return credito.carteira().saldo();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Dinheiro transferir(UUID carteiraOrigemId, UUID carteiraDestinoId, Dinheiro valor, UUID transacaoId) {
        if (carteiraOrigemId.equals(carteiraDestinoId)) {
            throw new IllegalArgumentException("origem e destino devem ser carteiras diferentes");
        }

        // Se A→B bloqueasse A e depois B, enquanto B→A bloqueasse B e depois A, cada transação
        // esperaria pela outra para sempre (deadlock). Bloqueando sempre na mesma ordem (menor id
        // primeiro), quem chegar depois apenas espera a vez.
        Carteira origem;
        Carteira destino;
        if (carteiraOrigemId.compareTo(carteiraDestinoId) < 0) {
            origem = bloquear(carteiraOrigemId);
            destino = bloquear(carteiraDestinoId);
        } else {
            destino = bloquear(carteiraDestinoId);
            origem = bloquear(carteiraOrigemId);
        }

        // Com as duas carteiras bloqueadas, nenhuma outra operação altera o saldo da origem entre a
        // verificação de saldo suficiente e o débito
        Movimentacao debito = origem.debitar(valor, transacaoId, relogio);
        Movimentacao credito = destino.creditar(valor, transacaoId, relogio);
        registrar(debito);
        registrar(credito);
        return debito.carteira().saldo();
    }

    private Carteira bloquear(UUID carteiraId) {
        return carteiras.buscarPorIdComBloqueio(carteiraId).orElseThrow(CarteiraNaoEncontradaException::new);
    }

    private void registrar(Movimentacao movimentacao) {
        carteiras.atualizar(movimentacao.carteira());
        lancamentos.registrar(movimentacao.lancamento());
    }
}
