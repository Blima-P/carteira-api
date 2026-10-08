package com.braga.carteiradigital.carteira.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

class CarteiraTest {

    private static final Instant CRIACAO = Instant.parse("2026-10-01T12:00:00Z");
    private static final Instant DEPOIS = Instant.parse("2026-10-02T08:30:00Z");

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID transacaoId = UUID.randomUUID();

    @Test
    void deveNascerComSaldoZero() {
        Carteira carteira = Carteira.nova(usuarioId, relogioEm(CRIACAO));

        assertThat(carteira.saldo()).isEqualTo(Dinheiro.ZERO);
        assertThat(carteira.usuarioId()).isEqualTo(usuarioId);
        assertThat(carteira.criadoEm()).isEqualTo(CRIACAO).isEqualTo(carteira.atualizadoEm());
    }

    @Test
    void creditoDeveSomarAoSaldoEGerarLancamento() {
        Carteira carteira = comSaldo("100.00");

        Movimentacao movimentacao = carteira.creditar(Dinheiro.de("25.50"), transacaoId, relogioEm(DEPOIS));

        assertThat(movimentacao.carteira().saldo()).isEqualTo(Dinheiro.de("125.50"));
        assertThat(movimentacao.carteira().atualizadoEm()).isEqualTo(DEPOIS);
        assertThat(movimentacao.carteira().criadoEm()).isEqualTo(CRIACAO);
        assertThat(movimentacao.lancamento()).isEqualTo(new Lancamento(transacaoId, carteira.id(),
                Lancamento.Natureza.CREDITO, Dinheiro.de("25.50"), Dinheiro.de("125.50"), DEPOIS));
    }

    @Test
    void creditoNaoDeveAlterarACarteiraOriginal() {
        Carteira carteira = comSaldo("100.00");

        carteira.creditar(Dinheiro.de("25.50"), transacaoId, relogioEm(DEPOIS));

        assertThat(carteira.saldo()).isEqualTo(Dinheiro.de("100.00"));
    }

    @Test
    void deveRecusarCreditoZeroOuNegativo() {
        Carteira carteira = comSaldo("100.00");

        assertThatThrownBy(() -> carteira.creditar(Dinheiro.ZERO, transacaoId, relogioEm(DEPOIS)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> carteira.creditar(Dinheiro.de("-1.00"), transacaoId, relogioEm(DEPOIS)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void naoDeveExistirCarteiraComSaldoNegativo() {
        assertThatThrownBy(() -> comSaldo("-0.01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }

    private Carteira comSaldo(String saldo) {
        return new Carteira(UUID.randomUUID(), usuarioId, Dinheiro.de(saldo), CRIACAO, CRIACAO);
    }

    private static Clock relogioEm(Instant instante) {
        return Clock.fixed(instante, ZoneOffset.UTC);
    }
}
